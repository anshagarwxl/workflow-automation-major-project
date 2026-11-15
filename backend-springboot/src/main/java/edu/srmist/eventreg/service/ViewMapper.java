package edu.srmist.eventreg.service;

import edu.srmist.eventreg.domain.*;
import edu.srmist.eventreg.repository.ApprovedTeamRepository;
import edu.srmist.eventreg.repository.PoolEntryRepository;
import edu.srmist.eventreg.repository.RegistrationRepository;
import edu.srmist.eventreg.web.dto.Views.*;
import org.springframework.stereotype.Component;

/** Maps JPA entities to API views. */
@Component
public class ViewMapper {

  private final ApprovedTeamRepository approvedTeams;
  private final RegistrationRepository registrations;
  private final PoolEntryRepository pool;

  public ViewMapper(ApprovedTeamRepository approvedTeams, RegistrationRepository registrations, PoolEntryRepository pool) {
    this.approvedTeams = approvedTeams;
    this.registrations = registrations;
    this.pool = pool;
  }

  public RegistrationView registration(Registration r) {
    FacultyAdvisor fa = r.getFacultyAdvisor();
    String teamCode = r.getStatus() == RegistrationStatus.APPROVED
        ? approvedTeams.findByRegistrationId(r.getId()).map(ApprovedTeam::getTeamCode).orElse(null) : null;
    return new RegistrationView(r.getId(), r.getEvent().getId(), r.getEvent().getCode(), r.getEvent().getName(),
        r.getMode(), r.getSource(), r.getTeamName(), fa == null ? null : fa.getId(), fa == null ? null : fa.getName(),
        r.getStatus(), r.getStatusReason(), r.getProcessInstanceKey(), r.getAllocationRound(), r.getDecision(),
        r.getDecidedBy(), r.getDecisionRemarks(), r.getDecidedAt(), r.getCreatedAt(), r.getUpdatedAt(),
        r.getMembers().stream().map(m -> new MemberView(m.getRegNo(), m.getName(), m.getEmail())).toList(), teamCode);
  }

  public EventView event(Event e) {
    long regs = registrations.findByEventIdOrderByCreatedAtDesc(e.getId()).size();
    long pending = registrations.countByEventIdAndStatus(e.getId(), RegistrationStatus.PENDING_FA_REVIEW)
        + registrations.countByEventIdAndStatus(e.getId(), RegistrationStatus.ESCALATED);
    EventStats stats = new EventStats(regs, approvedTeams.countByEventId(e.getId()),
        pool.countByEventIdAndStatus(e.getId(), PoolStatus.WAITING), pending);
    return new EventView(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.getVenue(), e.getEventDate(),
        e.getRegistrationDeadline(), e.getMinTeamSize(), e.getMaxTeamSize(), e.getMaxAllocationRounds(), e.getStatus(),
        e.getCoordinatorName(), e.getCoordinatorEmail(), e.getAllocationProcessInstanceKey(), e.getClosedAt(), stats);
  }

  public StudentView student(Student s) {
    FacultyAdvisor fa = s.getFacultyAdvisor();
    return new StudentView(s.getRegNo(), s.getName(), s.getEmail(), s.getDepartment(), s.getYearOfStudy(), s.getSection(),
        s.getEnrollmentStatus(), fa == null ? null : fa.getId(), fa == null ? null : fa.getName());
  }

  public AdvisorView advisor(FacultyAdvisor a) {
    return new AdvisorView(a.getId(), a.getName(), a.getEmail(), a.getDepartment());
  }

  public PoolEntryView pool(PoolEntry p) {
    return new PoolEntryView(p.getId(), p.getRegNo(), p.getName(), p.getEmail(), p.getFacultyAdvisorId(), p.getStatus(),
        p.getSourceRegistrationId(), p.getAllocatedRegistrationId(), p.getTimesAllocated(), p.getAddedAt(), p.getUpdatedAt());
  }

  public ApprovedTeamView approvedTeam(ApprovedTeam t) {
    return new ApprovedTeamView(t.getId(), t.getTeamCode(), t.getEvent().getId(), t.getEvent().getName(),
        t.getRegistrationId(), t.getTeamName(), t.getSource(), t.getFacultyAdvisorId(), t.getApprovedBy(), t.isEscalated(),
        t.getRemarks(), t.getApprovedAt(),
        t.getMembers().stream().map(m -> new MemberView(m.getRegNo(), m.getName(), m.getEmail())).toList());
  }

  public TimelineEntry timeline(WorkflowEvent e) {
    return new TimelineEntry(e.getId(), e.getStep(), e.getTitle(), e.getDetail(), e.getActor(), e.getLevel(), e.getOccurredAt());
  }

  public NotificationView notification(NotificationLog n) {
    return new NotificationView(n.getId(), n.getRegistrationId(), n.getTemplate(), n.getRecipientRegNo(),
        n.getRecipientEmail(), n.getStatus(), n.getDetail(), n.getSentAt());
  }

  public AllocationRunView run(AllocationRun r) {
    return new AllocationRunView(r.getId(), r.getRound(), r.getStudentsInPool(), r.getTeamsCreated(),
        r.getStudentsLeftOver(), r.getCreatedAt());
  }
}
