package edu.srmist.eventreg.worker;

import edu.srmist.eventreg.service.RegistrationWorkflowService;
import edu.srmist.eventreg.worker.WorkerVariables.RecordInvalidInput;
import edu.srmist.eventreg.worker.WorkerVariables.RecordRejectionInput;
import edu.srmist.eventreg.worker.WorkerVariables.SaveApprovedInput;
import edu.srmist.eventreg.workflow.Payloads.*;
import edu.srmist.eventreg.workflow.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Job workers of the Event Team Registration process. */
@Component
public class RegistrationWorkers {

  private static final Logger log = LoggerFactory.getLogger(RegistrationWorkers.class);

  private final RegistrationWorkflowService service;

  public RegistrationWorkers(RegistrationWorkflowService service) {
    this.service = service;
  }

  /** Service task "Validate registration rules". Throws REGISTRATION_INVALID (ZeebeBpmnError) on rule violations. */
  @JobWorker(type = ProcessConstants.JOB_VALIDATE_REGISTRATION, fetchVariables = "registration")
  public ValidationResult validateRegistration(@Variable(name = "registration") RegistrationPayload registration,
                                               ActivatedJob job) {
    log.info("[{}] validating {}", job.getProcessInstanceKey(), registration.registrationId());
    return service.validate(registration, job.getProcessInstanceKey());
  }

  /** Service task "Record invalid registration". */
  @JobWorker(type = ProcessConstants.JOB_RECORD_INVALID, fetchVariables = {"registration", "invalidReason", "memberChecks"})
  public InvalidRecorded recordInvalid(@VariablesAsType RecordInvalidInput input, ActivatedJob job) {
    return service.recordInvalid(input.registration(), input.invalidReason(), input.memberChecks(), job.getProcessInstanceKey());
  }

  /** Service task "Add student to unallocated pool". */
  @JobWorker(type = ProcessConstants.JOB_ADD_TO_POOL, fetchVariables = "registration")
  public PoolResult addToPool(@Variable(name = "registration") RegistrationPayload registration, ActivatedJob job) {
    return service.addToPool(registration, job.getProcessInstanceKey());
  }

  /** Service task "Save team to Approved Teams DB". Throws MEMBER_CONFLICT (ZeebeBpmnError) on a conflict. */
  @JobWorker(type = ProcessConstants.JOB_SAVE_APPROVED_TEAM,
      fetchVariables = {"registration", "decidedBy", "faRemarks", "escalated"})
  public SaveResult saveApprovedTeam(@VariablesAsType SaveApprovedInput input, ActivatedJob job) {
    return service.saveApprovedTeam(input.registration(), input.decidedBy(), input.faRemarks(),
        Boolean.TRUE.equals(input.escalated()), job.getProcessInstanceKey());
  }

  /** Service task "Record rejection". The BPMN input mapping prepares rejectionRemarks with FEEL. */
  @JobWorker(type = ProcessConstants.JOB_RECORD_REJECTION, fetchVariables = {"registration", "rejectionRemarks", "decidedBy"})
  public RejectionResult recordRejection(@VariablesAsType RecordRejectionInput input, ActivatedJob job) {
    return service.recordRejection(input.registration(), input.rejectionRemarks(), input.decidedBy(), job.getProcessInstanceKey());
  }

  /**
   * Task listener (creating / assigning) on every user task. Keeps the portal's registration status in step
   * with the task lifecycle. Completes without variables, as task listeners require.
   */
  @JobWorker(type = ProcessConstants.JOB_USER_TASK_LIFECYCLE, fetchVariables = "registration")
  public void onUserTaskLifecycle(@Variable(name = "registration", optional = true) RegistrationPayload registration,
                                  ActivatedJob job) {
    String eventType = job.getListenerEventType() == null ? "" : job.getListenerEventType().name();
    String assignee = job.getUserTask() == null ? null : job.getUserTask().getAssignee();
    service.onUserTaskEvent(job.getElementId(), eventType, assignee, registration, job.getProcessInstanceKey());
  }
}
