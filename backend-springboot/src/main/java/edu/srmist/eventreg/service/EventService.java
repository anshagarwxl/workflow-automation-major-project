package edu.srmist.eventreg.service;

import edu.srmist.eventreg.domain.*;
import edu.srmist.eventreg.repository.*;
import edu.srmist.eventreg.web.ApiException;
import edu.srmist.eventreg.web.dto.Requests.CreateEventRequest;
import edu.srmist.eventreg.web.dto.Views.*;
import edu.srmist.eventreg.workflow.SlaService;
import edu.srmist.eventreg.workflow.WorkflowEngine;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Events, their unallocated pool, approved teams and the Random Team Allocation trigger. */
@Service
public class EventService {

  private final EventRepository events;
  private final PoolEntryRepository pool;
  private final ApprovedTeamRepository approvedTeams;
  private final RegistrationRepository registrations;
  private final AllocationRunRepository runs;
  private final WorkflowEngine engine;
  private final SlaService sla;
  private final AuditService audit;
  private final ViewMapper views;
  private final TrackerService tracker;
  private final TransactionTemplate tx;

  public EventService(EventRepository events, PoolEntryRepository pool, ApprovedTeamRepository approvedTeams,
      RegistrationRepository registrations, AllocationRunRepository runs, WorkflowEngine engine, SlaService sla,
      AuditService audit, ViewMapper views, TrackerService tracker, TransactionTemplate tx) {
    this.events = events;
    this.pool = pool;
    this.approvedTeams = approvedTeams;
    this.registrations = registrations;
    this.runs = runs;
    this.engine = engine;
    this.sla = sla;
    this.audit = audit;
    this.views = views;
    this.tracker = tracker;
    this.tx = tx;
  }

  @Transactional(readOnly = true)
  public List<EventView> list() {
    return events.findAllByOrderByRegistrationDeadlineAsc().stream().map(views::event).toList();
  }

  @Transactional(readOnly = true)
  public EventView get(Long id) {
    return views.event(find(id));
  }

  @Transactional
  public EventView create(CreateEventRequest r) {
    if (r.minTeamSize() > r.maxTeamSize()) {
      throw ApiException.badRequest("Minimum team size cannot be larger than the maximum");
    }
    if (events.findByCode(r.code()).isPresent()) {
      throw ApiException.conflict("An event with code " + r.code() + " already exists");
    }
    Event e = new Event();
    e.setCode(r.code());
    e.setName(r.name());
    e.setDescription(r.description());
    e.setVenue(r.venue());
    e.setEventDate(r.eventDate());
    e.setRegistrationDeadline(r.registrationDeadline());
    e.setMinTeamSize(r.minTeamSize());
    e.setMaxTeamSize(r.maxTeamSize());
    e.setMaxAllocationRounds(r.maxAllocationRounds() == null ? 3 : r.maxAllocationRounds());
    e.setCoordinatorName(r.coordinatorName());
    e.setCoordinatorEmail(r.coordinatorEmail() == null ? "coordinator@eventreg.local" : r.coordinatorEmail());
    return views.event(events.save(e));
  }

  /** Closes registration and publishes the 'registration-closed' message that starts Random Team Allocation. */
  public EventView closeRegistration(Long id, String actor) {
    // Commit the status change first: the allocation workers start as soon as the message is published
    Event closed = tx.execute(s -> {
      Event e = find(id);
      if (e.getStatus() != EventStatus.OPEN) {
        throw ApiException.conflict("Registration for " + e.getName() + " is already closed");
      }
      e.setStatus(EventStatus.CLOSED);
      e.setClosedAt(OffsetDateTime.now());
      return e;
    });
    try {
      long messageKey = engine.publishRegistrationClosed(closed, sla.current());
      audit.record(null, id, null, "REGISTRATION_CLOSED", "Registration closed",
          "Message 'registration-closed' published (key " + messageKey + "), Random Team Allocation starts",
          actor == null ? "EVENT_COORDINATOR" : actor, "info");
    } catch (RuntimeException ex) {
      tx.executeWithoutResult(s -> find(id).setStatus(EventStatus.OPEN));
      throw ex;
    }
    return tx.execute(s -> views.event(find(id)));
  }

  @Transactional(readOnly = true)
  public List<PoolEntryView> pool(Long eventId) {
    find(eventId);
    return pool.findByEventIdOrderByAddedAtAsc(eventId).stream().map(views::pool).toList();
  }

  @Transactional(readOnly = true)
  public List<ApprovedTeamView> approvedTeams(Long eventId) {
    List<ApprovedTeam> teams = eventId == null ? approvedTeams.findAllByOrderByApprovedAtDesc()
        : approvedTeams.findByEventIdOrderByApprovedAtDesc(eventId);
    return teams.stream().map(views::approvedTeam).toList();
  }

  @Transactional(readOnly = true)
  public AllocationView allocation(Long eventId) {
    Event e = find(eventId);
    List<RegistrationView> randomTeams = registrations.findByEventIdOrderByCreatedAtDesc(eventId).stream()
        .filter(r -> r.getSource() == RegistrationSource.RANDOM).map(views::registration).toList();
    TrackerView t = e.getAllocationProcessInstanceKey() == null ? null
        : tracker.forProcessInstance(e.getAllocationProcessInstanceKey(), null);
    return new AllocationView(views.event(e), runs.findByEventIdOrderByRoundAsc(eventId).stream().map(views::run).toList(),
        randomTeams, pool(eventId), t);
  }

  private Event find(Long id) {
    return events.findById(id).orElseThrow(() -> ApiException.notFound("Event " + id));
  }
}
