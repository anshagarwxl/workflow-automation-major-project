package edu.srmist.eventreg.service;

import edu.srmist.eventreg.domain.*;
import edu.srmist.eventreg.repository.*;
import edu.srmist.eventreg.web.ApiException;
import edu.srmist.eventreg.web.dto.Requests.CreateRegistrationRequest;
import edu.srmist.eventreg.web.dto.Requests.MemberRequest;
import edu.srmist.eventreg.web.dto.Views.RegistrationView;
import edu.srmist.eventreg.workflow.SlaService;
import edu.srmist.eventreg.workflow.WorkflowEngine;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Submits registrations and starts one Event Team Registration process instance per registration. */
@Service
public class RegistrationService {

  private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

  private final RegistrationRepository registrations;
  private final EventRepository events;
  private final StudentRepository students;
  private final FacultyAdvisorRepository advisors;
  private final WorkflowEngine engine;
  private final SlaService sla;
  private final AuditService audit;
  private final ViewMapper views;
  private final TransactionTemplate tx;

  public RegistrationService(RegistrationRepository registrations, EventRepository events, StudentRepository students,
      FacultyAdvisorRepository advisors, WorkflowEngine engine, SlaService sla, AuditService audit, ViewMapper views,
      TransactionTemplate tx) {
    this.registrations = registrations;
    this.events = events;
    this.students = students;
    this.advisors = advisors;
    this.engine = engine;
    this.sla = sla;
    this.audit = audit;
    this.views = views;
    this.tx = tx;
  }

  /**
   * Saves the registration (committed first, so the job workers can find it), then starts the process.
   * Business rules are deliberately enforced inside the process by the validate-registration worker.
   */
  public RegistrationView submit(CreateRegistrationRequest request) {
    if (request.mode() == RegistrationMode.INDIVIDUAL && request.members().size() != 1) {
      throw ApiException.badRequest("An individual registration must list exactly one student");
    }
    Registration saved = tx.execute(status -> {
      Event event = events.findById(request.eventId()).orElseThrow(() -> ApiException.notFound("Event " + request.eventId()));
      Registration r = new Registration();
      r.setId(RegistrationWorkflowService.newRegistrationId());
      r.setEvent(event);
      r.setMode(request.mode());
      r.setSource(RegistrationSource.SELF_FORMED);
      r.setTeamName(request.mode() == RegistrationMode.TEAM && request.teamName() != null ? request.teamName().trim() : null);
      r.setSubmittedBy(request.submittedBy());
      for (MemberRequest m : request.members()) {
        r.addMember(new RegistrationMember(m.regNo().trim().toUpperCase(Locale.ROOT), m.name().trim(), m.email().trim().toLowerCase(Locale.ROOT)));
      }
      if (request.mode() == RegistrationMode.TEAM) {
        if (request.facultyAdvisorId() != null && !request.facultyAdvisorId().isBlank()) {
          r.setFacultyAdvisor(advisors.findById(request.facultyAdvisorId())
              .orElseThrow(() -> ApiException.badRequest("Unknown Faculty Advisor " + request.facultyAdvisorId())));
        }
      } else {
        students.findById(r.getMembers().get(0).getRegNo()).map(Student::getFacultyAdvisor).ifPresent(r::setFacultyAdvisor);
      }
      registrations.save(r);
      audit.record(r.getId(), event.getId(), null, "SUBMITTED",
          request.mode() == RegistrationMode.TEAM ? "Team registration submitted" : "Individual registration submitted",
          (r.getTeamName() == null ? "" : r.getTeamName() + " - ") + r.getMembers().size() + " member(s)",
          request.submittedBy() == null ? "STUDENT" : request.submittedBy(), "info");
      return r;
    });

    try {
      long key = engine.startRegistration(RegistrationWorkflowService.toPayload(saved), sla.current(),
          saved.getEvent().getCoordinatorEmail());
      return tx.execute(status -> {
        Registration r = registrations.findById(saved.getId()).orElseThrow();
        r.setProcessInstanceKey(key);
        audit.record(r.getId(), r.getEvent().getId(), key, "PROCESS_STARTED", "Workflow started",
            "Event Team Registration instance " + key, "SYSTEM", "info");
        return views.registration(r);
      });
    } catch (RuntimeException e) {
      log.error("Could not start the workflow for {}", saved.getId(), e);
      tx.executeWithoutResult(status -> registrations.findById(saved.getId()).ifPresent(r -> {
        r.changeStatus(RegistrationStatus.INVALID, "The workflow engine is unavailable, please submit again");
        audit.record(r.getId(), r.getEvent().getId(), null, "ENGINE_UNAVAILABLE", "Workflow could not start",
            e.getMessage(), "SYSTEM", "error");
      }));
      throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "The workflow engine is unavailable. Your registration was not started, please try again.");
    }
  }

  @Transactional(readOnly = true)
  public List<RegistrationView> list(Long eventId, RegistrationStatus status, String regNo) {
    List<Registration> result;
    if (regNo != null && !regNo.isBlank()) {
      result = registrations.findByMember(regNo.trim().toUpperCase(Locale.ROOT));
    } else if (eventId != null) {
      result = registrations.findByEventIdOrderByCreatedAtDesc(eventId);
    } else {
      result = registrations.findAllByOrderByCreatedAtDesc();
    }
    return result.stream()
        .filter(r -> eventId == null || r.getEvent().getId().equals(eventId))
        .filter(r -> status == null || r.getStatus() == status)
        .map(views::registration).toList();
  }

  @Transactional(readOnly = true)
  public RegistrationView get(String id) {
    return registrations.findById(id).map(views::registration).orElseThrow(() -> ApiException.notFound("Registration " + id));
  }
}
