package edu.srmist.eventreg.worker;

import edu.srmist.eventreg.service.RegistrationWorkflowService;
import edu.srmist.eventreg.worker.WorkerVariables.RecordNotificationInput;
import edu.srmist.eventreg.workflow.Payloads.NotificationResult;
import edu.srmist.eventreg.workflow.ProcessConstants;
import io.camunda.client.annotation.CustomHeaders;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.api.response.ActivatedJob;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Job worker of the notify-team child process. The emails themselves are sent by the Email connector. */
@Component
public class NotificationWorkers {

  private final RegistrationWorkflowService service;

  public NotificationWorkers(RegistrationWorkflowService service) {
    this.service = service;
  }

  /** "Log delivered notification" / "Log failed notification": the task header 'outcome' tells them apart. */
  @JobWorker(type = ProcessConstants.JOB_RECORD_NOTIFICATION, fetchVariables = {"notification", "deliveries", "deliveryError"})
  public NotificationResult recordNotification(@VariablesAsType RecordNotificationInput input,
                                               @CustomHeaders Map<String, String> headers, ActivatedJob job) {
    String outcome = headers.getOrDefault("outcome", "DELIVERED");
    return service.recordNotification(input.notification(), input.deliveries(), input.deliveryError(), outcome,
        job.getProcessInstanceKey());
  }
}
