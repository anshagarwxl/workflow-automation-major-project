package edu.srmist.eventreg.worker;

import edu.srmist.eventreg.service.RegistrationWorkflowService;
import edu.srmist.eventreg.worker.WorkerVariables.AllocateInput;
import edu.srmist.eventreg.worker.WorkerVariables.FetchUnallocatedInput;
import edu.srmist.eventreg.workflow.Payloads.AllocationResult;
import edu.srmist.eventreg.workflow.Payloads.FetchResult;
import edu.srmist.eventreg.workflow.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.api.response.ActivatedJob;
import java.util.List;
import org.springframework.stereotype.Component;

/** Job workers of the Random Team Allocation process. */
@Component
public class AllocationWorkers {

  private final RegistrationWorkflowService service;

  public AllocationWorkers(RegistrationWorkflowService service) {
    this.service = service;
  }

  @JobWorker(type = ProcessConstants.JOB_FETCH_UNALLOCATED, fetchVariables = "eventId")
  public FetchResult fetchUnallocated(@VariablesAsType FetchUnallocatedInput input, ActivatedJob job) {
    return service.fetchUnallocated(input.eventId(), job.getProcessInstanceKey());
  }

  /** Throws INSUFFICIENT_STUDENTS (ZeebeBpmnError) when the pool is smaller than the minimum team size. */
  @JobWorker(type = ProcessConstants.JOB_ALLOCATE_RANDOM_TEAMS,
      fetchVariables = {"eventId", "unallocatedStudents", "minTeamSize", "maxTeamSize", "allocationRound"})
  public AllocationResult allocate(@VariablesAsType AllocateInput input, ActivatedJob job) {
    return service.allocate(input.eventId(),
        input.unallocatedStudents() == null ? List.of() : input.unallocatedStudents(),
        input.minTeamSize(), input.maxTeamSize(),
        input.allocationRound() == null ? 1 : input.allocationRound(),
        job.getProcessInstanceKey());
  }
}
