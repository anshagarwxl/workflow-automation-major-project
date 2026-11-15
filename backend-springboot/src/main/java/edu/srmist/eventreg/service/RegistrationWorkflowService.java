package edu.srmist.eventreg.service;

import edu.srmist.eventreg.config.SimulationSettings;
import edu.srmist.eventreg.domain.*;
import edu.srmist.eventreg.repository.*;
import edu.srmist.eventreg.workflow.Payloads.*;
import edu.srmist.eventreg.workflow.ProcessConstants;
import io.camunda.client.exception.BpmnError;
import io.camunda.zeebe.spring.common.exception.ZeebeBpmnError;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic behind the job workers. Each public method runs in one database transaction; business
 * failures are raised as {@link ZeebeBpmnError} so the engine routes them to the matching error boundary
 * event, while technical failures propagate as exceptions and are retried by Camunda (then raise an incident).
 */
@Service
public class RegistrationWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(RegistrationWorkflowService.class);

  private final RegistrationRepository registrations;
  private final EventRepository events;
  private final StudentRepository students;
  private final FacultyAdvisorRepository advisors;
  private final PoolEntryRepository pool;
  private final ApprovedTeamRepository approvedTeams;
  private final NotificationLogRepository notifications;
  private final AllocationRunRepository allocationRuns;
  private final AuditService audit;
  private final SimulationSettings simulation;
  private final Random random = new Random();

  public RegistrationWorkflowService(RegistrationRepository registrations, EventRepository events,
      StudentRepository students, FacultyAdvisorRepository advisors, PoolEntryRepository pool,
      ApprovedTeamRepository approvedTeams, NotificationLogRepository notifications,
      AllocationRunRepository allocationRuns, AuditService audit, SimulationSettings simulation) {
    this.registrations = registrations;
    this.events = events;
    this.students = students;
    this.advisors = advisors;
    this.pool = pool;
    this.approvedTeams = approvedTeams;
    this.notifications = notifications;
    this.allocationRuns = allocationRuns;
    this.audit = audit;
    this.simulation = simulation;
  }

  // =============================================================== validate-registration

  @Transactional(noRollbackFor = BpmnError.class) // keep the audit entry written before the BPMN error
  public ValidationResult validate(RegistrationPayload payload, long processInstanceKey) {
    Registration registration = load(payload.registrationId());
    registration.setProcessInstanceKey(processInstanceKey);
    Event event = registration.getEvent();

    List<String> violations = new ArrayList<>(RegistrationRules.structuralViolations(payload, event, OffsetDateTime.now()));

    // Faculty Advisor must exist and advise at least one member (self-formed teams)
    if (payload.mode() == RegistrationMode.TEAM && payload.faId() != null && !payload.faId().isBlank()) {
      if (advisors.findById(payload.faId()).isEmpty()) {
        violations.add("Faculty Advisor " + payload.faId() + " does not exist");
      } else if (payload.source() == RegistrationSource.SELF_FORMED) {
        boolean advisesMember = payload.members().stream()
            .map(m -> students.findById(m.regNo()))
            .flatMap(Optional::stream)
            .anyMatch(s -> s.getFacultyAdvisor() != null && payload.faId().equals(s.getFacultyAdvisor().getId()));
        if (!advisesMember) {
          violations.add("The selected Faculty Advisor does not advise any member of this team");
        }
      }
    }

    // A student can only be in one active registration per event (earliest registration wins)
    if (payload.source() == RegistrationSource.SELF_FORMED) {
      for (MemberPayload m : payload.members()) {
        registrations.findOtherRegistrationsOfStudent(event.getId(), m.regNo(), RegistrationStatus.ACTIVE, registration.getId())
            .stream()
            .filter(other -> other.getCreatedAt().isBefore(registration.getCreatedAt()))
            .findFirst()
            .ifPresent(other -> violations.add("Student " + m.regNo() + " is already in registration " + other.getId()
                + " (" + other.getStatus().name().toLowerCase().replace('_', ' ') + ")"));
      }
      if (payload.mode() == RegistrationMode.TEAM && payload.teamName() != null
          && registrations.teamNameTaken(event.getId(), payload.teamName(), RegistrationStatus.ACTIVE, registration.getId())) {
        violations.add("Another team is already registered as \"" + payload.teamName() + "\"");
      }
    }
    for (MemberPayload m : payload.members()) {
      if (!approvedTeams.findByEventAndMember(event.getId(), m.regNo()).isEmpty()) {
        violations.add("Student " + m.regNo() + " is already in an approved team for this event");
      }
    }

    if (!violations.isEmpty()) {
      String reason = String.join("; ", violations);
      audit.record(registration.getId(), event.getId(), processInstanceKey, "RULES_FAILED", "Registration rules failed",
          reason, "SYSTEM", "error");
      throw new ZeebeBpmnError(ProcessConstants.ERROR_REGISTRATION_INVALID, reason,
          Map.of("invalidReason", reason, "invalidReasons", violations));
    }

    registration.changeStatus(RegistrationStatus.VERIFYING, null);
    audit.record(registration.getId(), event.getId(), processInstanceKey, "RULES_PASSED", "Registration rules passed",
        payload.members().size() + " member(s) checked: team size, duplicates, deadline, FA, existing registrations",
        "SYSTEM", "success");
    return new ValidationResult(true, payload.members().size());
  }

  // =============================================================== record-invalid-registration

  @Transactional
  public InvalidRecorded recordInvalid(RegistrationPayload payload, String invalidReason, List<MemberCheck> memberChecks,
                                       long processInstanceKey) {
    Registration registration = load(payload.registrationId());
    String reason = invalidReason == null || invalidReason.isBlank() ? "The registration failed verification" : invalidReason;
    registration.changeStatus(RegistrationStatus.INVALID, reason);

    if (registration.getSource() == RegistrationSource.RANDOM) {
      // Eligible members of an invalid random team go back to the pool; ineligible ones are removed
      Set<String> ineligible = memberChecks == null ? Set.of() : memberChecks.stream()
          .filter(c -> c != null && !Boolean.TRUE.equals(c.eligible()))
          .map(MemberCheck::regNo).collect(Collectors.toSet());
      for (PoolEntry entry : pool.findByEventIdAndRegNoIn(registration.getEvent().getId(), memberRegNos(registration))) {
        entry.moveTo(ineligible.contains(entry.getRegNo()) ? PoolStatus.REMOVED : PoolStatus.WAITING, null);
      }
    }
    audit.record(registration.getId(), registration.getEvent().getId(), processInstanceKey, "INVALID",
        "Registration marked invalid", reason, "SYSTEM", "error");
    return new InvalidRecorded(reason);
  }

  // =============================================================== add-to-unallocated-pool

  @Transactional
  public PoolResult addToPool(RegistrationPayload payload, long processInstanceKey) {
    Registration registration = load(payload.registrationId());
    Event event = registration.getEvent();
    MemberPayload student = payload.members().get(0);

    PoolEntry entry = pool.findByEventIdAndRegNo(event.getId(), student.regNo()).orElseGet(PoolEntry::new);
    entry.setEvent(event);
    entry.setRegNo(student.regNo());
    entry.setName(student.name());
    entry.setEmail(student.email());
    entry.setSourceRegistrationId(registration.getId());
    entry.setFacultyAdvisorId(students.findById(student.regNo())
        .map(Student::getFacultyAdvisor).map(FacultyAdvisor::getId).orElse(payload.faId()));
    entry.moveTo(PoolStatus.WAITING, null);
    pool.save(entry);

    registration.changeStatus(RegistrationStatus.POOLED, "Waiting for random team allocation");
    long poolSize = pool.countByEventIdAndStatus(event.getId(), PoolStatus.WAITING);
    audit.record(registration.getId(), event.getId(), processInstanceKey, "POOLED", "Added to the unallocated pool",
        poolSize + " student(s) now waiting for random allocation", "SYSTEM", "info");
    return new PoolResult(true, (int) poolSize);
  }

  // =============================================================== save-approved-team

  @Transactional(noRollbackFor = BpmnError.class) // keep the audit entry written before the BPMN error
  public SaveResult saveApprovedTeam(RegistrationPayload payload, String decidedBy, String remarks, boolean escalated,
                                     long processInstanceKey) {
    Registration registration = load(payload.registrationId());
    Event event = registration.getEvent();

    Optional<ApprovedTeam> existing = approvedTeams.findByRegistrationId(registration.getId());
    if (existing.isPresent()) { // idempotent on job retry
      return new SaveResult(toRef(existing.get()));
    }

    List<String> conflicts = new ArrayList<>();
    for (MemberPayload m : payload.members()) {
      approvedTeams.findByEventAndMember(event.getId(), m.regNo()).forEach(t ->
          conflicts.add(m.regNo() + " is already in approved team " + t.getTeamCode()));
    }
    if (conflicts.isEmpty() && simulation.consumeForcedConflict()) {
      conflicts.add(payload.members().get(0).regNo() + " was approved in another team at the same time (simulated race condition)");
    }
    if (!conflicts.isEmpty()) {
      String message = String.join("; ", conflicts);
      audit.record(registration.getId(), event.getId(), processInstanceKey, "MEMBER_CONFLICT",
          "Approval blocked: member conflict", message, "SYSTEM", "error");
      throw new ZeebeBpmnError(ProcessConstants.ERROR_MEMBER_CONFLICT, message, Map.of("conflictMessage", message));
    }

    ApprovedTeam team = new ApprovedTeam();
    team.setTeamCode(event.getCode() + "-" + registration.getId().substring(4));
    team.setEvent(event);
    team.setRegistrationId(registration.getId());
    team.setTeamName(registration.getTeamName());
    team.setSource(registration.getSource());
    team.setFacultyAdvisorId(payload.faId());
    team.setApprovedBy(decidedBy);
    team.setEscalated(escalated);
    team.setRemarks(remarks);
    registration.getMembers().forEach(m -> team.getMembers().add(new ApprovedTeamMember(m.getRegNo(), m.getName(), m.getEmail())));
    approvedTeams.save(team);

    registration.changeStatus(RegistrationStatus.APPROVED, "Approved by " + (decidedBy == null ? "reviewer" : decidedBy));
    for (PoolEntry entry : pool.findByEventIdAndRegNoIn(event.getId(), memberRegNos(registration))) {
      entry.moveTo(PoolStatus.PLACED, registration.getId());
      updateSourceRegistration(entry, RegistrationStatus.PLACED,
          "Placed in random team " + registration.getTeamName() + " (" + team.getTeamCode() + ")", processInstanceKey);
    }
    audit.record(registration.getId(), event.getId(), processInstanceKey, "APPROVED_SAVED",
        "Saved to Approved Teams DB", "Team code " + team.getTeamCode(), "SYSTEM", "success");
    return new SaveResult(toRef(team));
  }

  // =============================================================== record-rejection

  @Transactional
  public RejectionResult recordRejection(RegistrationPayload payload, String rejectionRemarks, String decidedBy,
                                         long processInstanceKey) {
    Registration registration = load(payload.registrationId());
    String remarks = rejectionRemarks == null || rejectionRemarks.isBlank() ? "No remarks provided" : rejectionRemarks;
    registration.changeStatus(RegistrationStatus.REJECTED, remarks);
    if (registration.getDecisionRemarks() == null) {
      registration.setDecisionRemarks(remarks);
    }
    boolean returned = false;
    if (registration.getSource() == RegistrationSource.RANDOM) {
      for (PoolEntry entry : pool.findByEventIdAndRegNoIn(registration.getEvent().getId(), memberRegNos(registration))) {
        if (entry.getStatus() == PoolStatus.ALLOCATED) {
          entry.moveTo(PoolStatus.WAITING, null);
          updateSourceRegistration(entry, null,
              "Back in the pool: random team " + registration.getTeamName() + " was not approved", null);
          returned = true;
        }
      }
    }
    audit.record(registration.getId(), registration.getEvent().getId(), processInstanceKey, "REJECTED",
        "Rejection recorded", remarks + (returned ? " (members returned to the unallocated pool)" : ""),
        decidedBy == null ? "SYSTEM" : decidedBy, "warning");
    return new RejectionResult(remarks, returned);
  }

  // =============================================================== record-notification

  @Transactional
  public NotificationResult recordNotification(NotificationPayload notification, List<Delivery> deliveries,
                                               String deliveryError, String outcome, long processInstanceKey) {
    boolean delivered = "DELIVERED".equals(outcome);
    List<MemberPayload> recipients = notification.recipients() == null ? List.of() : notification.recipients();
    for (MemberPayload r : recipients) {
      NotificationLog entry = new NotificationLog();
      entry.setRegistrationId(notification.registrationId());
      entry.setTemplate(notification.template());
      entry.setRecipientRegNo(r.regNo());
      entry.setRecipientEmail(r.email());
      entry.setStatus(delivered ? NotificationStatus.DELIVERED : NotificationStatus.FAILED);
      entry.setDetail(delivered ? "Sent by the Email connector" : deliveryError);
      entry.setProcessInstanceKey(processInstanceKey);
      notifications.save(entry);
    }
    Long eventId = registrations.findById(notification.registrationId()).map(r -> r.getEvent().getId()).orElse(null);
    audit.record(notification.registrationId(), eventId, processInstanceKey,
        delivered ? "NOTIFIED" : "NOTIFICATION_FAILED",
        delivered ? "Email sent: " + humanTemplate(notification.template()) : "Email delivery failed",
        delivered ? recipients.size() + " recipient(s), sent one after another" : deliveryError,
        "SYSTEM", delivered ? "success" : "warning");
    return new NotificationResult(delivered ? "DELIVERED" : "FAILED", recipients.size());
  }

  // =============================================================== fetch-unallocated-students

  @Transactional
  public FetchResult fetchUnallocated(long eventId, long processInstanceKey) {
    Event event = events.findById(eventId).orElseThrow(() -> new IllegalStateException("Unknown event " + eventId));
    if (event.getAllocationProcessInstanceKey() == null) {
      event.setAllocationProcessInstanceKey(processInstanceKey);
    }
    if (event.getStatus() == EventStatus.CLOSED) {
      event.setStatus(EventStatus.ALLOCATING);
    }
    List<UnallocatedStudent> waiting = pool.findByEventIdAndStatusOrderByAddedAtAsc(eventId, PoolStatus.WAITING).stream()
        .map(p -> new UnallocatedStudent(p.getRegNo(), p.getName(), p.getEmail(), p.getFacultyAdvisorId()))
        .toList();
    if (waiting.isEmpty()) {
      event.setStatus(EventStatus.COMPLETED);
    }
    audit.record(null, eventId, processInstanceKey, "POOL_FETCHED", "Unallocated pool checked",
        waiting.size() + " student(s) without a team", "SYSTEM", "info");
    return new FetchResult(waiting.size(), waiting);
  }

  // =============================================================== allocate-random-teams

  @Transactional(noRollbackFor = BpmnError.class) // keep the audit entry written before the BPMN error
  public AllocationResult allocate(long eventId, List<UnallocatedStudent> unallocated, int minTeamSize, int maxTeamSize,
                                   int round, long processInstanceKey) {
    Event event = events.findById(eventId).orElseThrow(() -> new IllegalStateException("Unknown event " + eventId));
    AllocationPlanner.Plan plan;
    try {
      plan = AllocationPlanner.plan(unallocated, minTeamSize, maxTeamSize, random);
    } catch (AllocationPlanner.InsufficientStudentsException e) {
      audit.record(null, eventId, processInstanceKey, "ALLOCATION_BLOCKED", "Random allocation not possible",
          e.getMessage(), "SYSTEM", "error");
      throw new ZeebeBpmnError(ProcessConstants.ERROR_INSUFFICIENT_STUDENTS, e.getMessage(),
          Map.of("allocationError", e.getMessage()));
    }

    Map<String, PoolEntry> entries = pool.findByEventIdAndRegNoIn(eventId,
            unallocated.stream().map(UnallocatedStudent::regNo).toList()).stream()
        .collect(Collectors.toMap(PoolEntry::getRegNo, Function.identity()));

    List<RegistrationPayload> generated = new ArrayList<>();
    int index = 1;
    for (List<UnallocatedStudent> members : plan.teams()) {
      FacultyAdvisor fa = pickAdvisor(members);
      Registration r = new Registration();
      r.setId(newRegistrationId());
      r.setEvent(event);
      r.setMode(RegistrationMode.TEAM);
      r.setSource(RegistrationSource.RANDOM);
      r.setTeamName(String.format("%s Random R%d-%02d", event.getCode(), round, index++));
      r.setFacultyAdvisor(fa);
      r.setAllocationRound(round);
      r.setSubmittedBy("random-team-allocation");
      members.forEach(m -> r.addMember(new RegistrationMember(m.regNo(), m.name(), m.email())));
      registrations.save(r);
      members.forEach(m -> {
        PoolEntry e = entries.get(m.regNo());
        if (e != null) {
          e.moveTo(PoolStatus.ALLOCATED, r.getId());
          e.setTimesAllocated(e.getTimesAllocated() + 1);
          updateSourceRegistration(e, null, "Allocated to random team " + r.getTeamName()
              + " (round " + round + "), waiting for FA confirmation", null);
        }
      });
      audit.record(r.getId(), eventId, processInstanceKey, "RANDOM_TEAM_CREATED", "Random team created",
          "Round " + round + ": " + members.stream().map(UnallocatedStudent::regNo).collect(Collectors.joining(", "))
              + (fa == null ? "" : " - FA " + fa.getName()), "SYSTEM", "info");
      generated.add(toPayload(r));
    }

    AllocationRun run = new AllocationRun();
    run.setEvent(event);
    run.setRound(round);
    run.setStudentsInPool(unallocated.size());
    run.setTeamsCreated(generated.size());
    run.setStudentsLeftOver(plan.leftOver().size());
    run.setProcessInstanceKey(processInstanceKey);
    allocationRuns.save(run);
    audit.record(null, eventId, processInstanceKey, "ALLOCATION_ROUND", "Allocation round " + round,
        generated.size() + " team(s) from " + unallocated.size() + " student(s); " + plan.leftOver().size() + " left over",
        "SYSTEM", "success");
    return new AllocationResult(generated, plan.leftOver().size());
  }

  // =============================================================== user-task-lifecycle (task listener)

  @Transactional
  public void onUserTaskEvent(String elementId, String eventType, String assignee, RegistrationPayload payload,
                              long processInstanceKey) {
    if (payload == null || payload.registrationId() == null) {
      return;
    }
    Registration registration = registrations.findById(payload.registrationId()).orElse(null);
    if (registration == null) {
      return;
    }
    Long eventId = registration.getEvent().getId();
    if ("CREATING".equals(eventType)) {
      switch (elementId) {
        case ProcessConstants.TASK_FA_REVIEW -> {
          registration.changeStatus(RegistrationStatus.PENDING_FA_REVIEW, "Waiting for the Faculty Advisor's decision");
          audit.record(registration.getId(), eventId, processInstanceKey, "FA_REVIEW_CREATED",
              "Sent to Faculty Advisor for review", payload.faName() + " (" + payload.faId() + ")", "SYSTEM", "info");
        }
        case ProcessConstants.TASK_COORDINATOR_REVIEW -> {
          registration.changeStatus(RegistrationStatus.ESCALATED, "FA review overdue: escalated to the Event Coordinator");
          audit.record(registration.getId(), eventId, processInstanceKey, "ESCALATED",
              "Review escalated to Event Coordinator", "No FA decision within the SLA", "SYSTEM", "warning");
        }
        case ProcessConstants.TASK_MANUAL_VERIFICATION -> {
          registration.changeStatus(RegistrationStatus.MANUAL_VERIFICATION, "Student Records did not answer in time");
          audit.record(registration.getId(), eventId, processInstanceKey, "MANUAL_VERIFICATION",
              "Manual verification requested", "Verification timer fired; the coordinator verifies the members", "SYSTEM", "warning");
        }
        default -> { }
      }
    } else if ("ASSIGNING".equals(eventType) && assignee != null) {
      audit.record(registration.getId(), eventId, processInstanceKey, "TASK_ASSIGNED", "Task assigned",
          humanTask(elementId) + " assigned to " + assignee, assignee, "info");
    }
  }

  // =============================================================== helpers

  public static String newRegistrationId() {
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder sb = new StringBuilder("REG-");
    Random r = new Random();
    for (int i = 0; i < 6; i++) {
      sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
    }
    return sb.toString();
  }

  public static RegistrationPayload toPayload(Registration r) {
    FacultyAdvisor fa = r.getFacultyAdvisor();
    return new RegistrationPayload(
        r.getId(), r.getEvent().getId(), r.getEvent().getName(), r.getMode(), r.getSource(),
        r.getTeamName(),
        r.getMembers().stream().map(m -> new MemberPayload(m.getRegNo(), m.getName(), m.getEmail())).toList(),
        fa == null ? null : fa.getId(), fa == null ? null : fa.getName(), fa == null ? null : fa.getEmail(),
        r.getAllocationRound());
  }

  /** Keeps the student's own (individual) registration in step with what happens to their random team. */
  private void updateSourceRegistration(PoolEntry entry, RegistrationStatus status, String reason, Long processInstanceKey) {
    if (entry.getSourceRegistrationId() == null) {
      return;
    }
    registrations.findById(entry.getSourceRegistrationId()).ifPresent(source -> {
      source.changeStatus(status == null ? source.getStatus() : status, reason);
      audit.record(source.getId(), source.getEvent().getId(), processInstanceKey,
          status == RegistrationStatus.PLACED ? "PLACED" : "POOL_UPDATE", status == RegistrationStatus.PLACED ? "Placed in a team" : "Allocation update",
          reason, "SYSTEM", status == RegistrationStatus.PLACED ? "success" : "info");
    });
  }

  private FacultyAdvisor pickAdvisor(List<UnallocatedStudent> members) {
    // The FA who advises most members of the random team confirms it (ties: first member's FA)
    Map<String, Long> counts = members.stream().filter(m -> m.faId() != null)
        .collect(Collectors.groupingBy(UnallocatedStudent::faId, LinkedHashMap::new, Collectors.counting()));
    return counts.entrySet().stream().max(Map.Entry.comparingByValue())
        .flatMap(e -> advisors.findById(e.getKey()))
        .orElseGet(() -> advisors.findAllByOrderByNameAsc().stream().findFirst().orElse(null));
  }

  private Registration load(String registrationId) {
    return registrations.findById(registrationId)
        .orElseThrow(() -> new IllegalStateException("Registration " + registrationId + " not found (yet)"));
  }

  private static List<String> memberRegNos(Registration r) {
    return r.getMembers().stream().map(RegistrationMember::getRegNo).toList();
  }

  private static ApprovedTeamRef toRef(ApprovedTeam t) {
    return new ApprovedTeamRef(t.getTeamCode(), t.getId(), t.getApprovedAt().toString());
  }

  private static String humanTemplate(String template) {
    return switch (template == null ? "" : template) {
      case "TEAM_APPROVED" -> "team approved";
      case "TEAM_REJECTED" -> "team rejected";
      case "REGISTRATION_INVALID" -> "registration invalid";
      default -> String.valueOf(template);
    };
  }

  private static String humanTask(String elementId) {
    return switch (elementId) {
      case ProcessConstants.TASK_FA_REVIEW -> "FA review";
      case ProcessConstants.TASK_COORDINATOR_REVIEW -> "Escalated review";
      case ProcessConstants.TASK_MANUAL_VERIFICATION -> "Manual verification";
      default -> elementId;
    };
  }
}
