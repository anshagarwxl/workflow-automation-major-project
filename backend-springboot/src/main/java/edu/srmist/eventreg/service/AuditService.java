package edu.srmist.eventreg.service;

import edu.srmist.eventreg.domain.WorkflowEvent;
import edu.srmist.eventreg.repository.WorkflowEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes the audit trail shown on the live process tracker. */
@Service
public class AuditService {

  private final WorkflowEventRepository repository;

  public AuditService(WorkflowEventRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void record(String registrationId, Long eventId, Long processInstanceKey, String step, String title,
                     String detail, String actor, String level) {
    WorkflowEvent e = new WorkflowEvent();
    e.setRegistrationId(registrationId);
    e.setEventId(eventId);
    e.setProcessInstanceKey(processInstanceKey);
    e.setStep(step);
    e.setTitle(title);
    e.setDetail(detail == null ? null : (detail.length() > 1990 ? detail.substring(0, 1990) : detail));
    e.setActor(actor);
    e.setLevel(level);
    repository.save(e);
  }
}
