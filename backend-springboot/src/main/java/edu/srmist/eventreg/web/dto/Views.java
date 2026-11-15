package edu.srmist.eventreg.web.dto;

import edu.srmist.eventreg.domain.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Response bodies of the REST API. */
public final class Views {

  private Views() {}

  public record MemberView(String regNo, String name, String email) {}

  public record RegistrationView(
      String id, Long eventId, String eventCode, String eventName, RegistrationMode mode, RegistrationSource source,
      String teamName, String facultyAdvisorId, String facultyAdvisorName, RegistrationStatus status, String statusReason,
      Long processInstanceKey, Integer allocationRound, String decision, String decidedBy, String decisionRemarks,
      OffsetDateTime decidedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<MemberView> members,
      String teamCode) {}

  public record EventStats(long registrations, long approvedTeams, long poolWaiting, long pendingReview) {}

  public record EventView(
      Long id, String code, String name, String description, String venue, java.time.LocalDate eventDate,
      OffsetDateTime registrationDeadline, int minTeamSize, int maxTeamSize, int maxAllocationRounds, EventStatus status,
      String coordinatorName, String coordinatorEmail, Long allocationProcessInstanceKey, OffsetDateTime closedAt,
      EventStats stats) {}

  public record StudentView(String regNo, String name, String email, String department, int yearOfStudy, String section,
                            EnrollmentStatus enrollmentStatus, String facultyAdvisorId, String facultyAdvisorName) {}

  public record AdvisorView(String id, String name, String email, String department) {}

  public record PoolEntryView(Long id, String regNo, String name, String email, String facultyAdvisorId, PoolStatus status,
                              String sourceRegistrationId, String allocatedRegistrationId, int timesAllocated,
                              OffsetDateTime addedAt, OffsetDateTime updatedAt) {}

  public record ApprovedTeamView(Long id, String teamCode, Long eventId, String eventName, String registrationId,
                                 String teamName, RegistrationSource source, String facultyAdvisorId, String approvedBy,
                                 boolean escalated, String remarks, OffsetDateTime approvedAt, List<MemberView> members) {}

  public record TaskView(
      Long userTaskKey, String name, String elementId, String kind, String state, String assignee,
      List<String> candidateGroups, OffsetDateTime creationDate, OffsetDateTime completionDate, OffsetDateTime dueDate,
      Long processInstanceKey, String bpmnProcessId, String formId, RegistrationView registration) {}

  public record TaskDetailView(TaskView task, Object memberChecks, Object sla, Map<String, Object> formSchema) {}

  public record TimelineEntry(Long id, String step, String title, String detail, String actor, String level,
                              OffsetDateTime occurredAt) {}

  public record NotificationView(Long id, String registrationId, String template, String recipientRegNo,
                                 String recipientEmail, NotificationStatus status, String detail, OffsetDateTime sentAt) {}

  public record ElementView(String elementId, String elementName, String type, String state, OffsetDateTime startDate,
                            OffsetDateTime endDate, boolean incident) {}

  public record IncidentView(Long incidentKey, String elementId, String errorType, String errorMessage,
                             OffsetDateTime creationTime, String state) {}

  public record InstanceView(Long processInstanceKey, String bpmnProcessId, String processName, Integer version,
                             Long processDefinitionKey, String state, boolean hasIncident, OffsetDateTime startDate,
                             OffsetDateTime endDate, Long parentProcessInstanceKey) {}

  public record TrackerView(
      InstanceView instance,
      List<String> activeElements,
      List<String> completedElements,
      List<String> incidentElements,
      List<String> takenSequenceFlows,
      List<ElementView> elements,
      List<IncidentView> incidents,
      List<InstanceView> children,
      List<TaskView> openTasks,
      List<TimelineEntry> timeline,
      List<NotificationView> notifications,
      RegistrationView registration,
      String operateUrl) {}

  public record AllocationRunView(Long id, int round, int studentsInPool, int teamsCreated, int studentsLeftOver,
                                  OffsetDateTime createdAt) {}

  public record AllocationView(EventView event, List<AllocationRunView> runs, List<RegistrationView> randomTeams,
                               List<PoolEntryView> pool, TrackerView tracker) {}

  public record DashboardView(Map<String, Long> registrationsByStatus, long approvedTeams, long poolWaiting,
                              long openTasks, long incidents, List<EventView> events, List<TimelineEntry> recentActivity,
                              Map<String, Object> engine) {}

  public record SettingsView(String slaPreset, Object sla, boolean studentRecordsOutage, boolean forceMemberConflict,
                             String mailboxUrl, String operateUrl) {}
}
