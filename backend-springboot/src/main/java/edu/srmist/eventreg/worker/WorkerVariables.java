package edu.srmist.eventreg.worker;

import edu.srmist.eventreg.workflow.Payloads.Delivery;
import edu.srmist.eventreg.workflow.Payloads.MemberCheck;
import edu.srmist.eventreg.workflow.Payloads.NotificationPayload;
import edu.srmist.eventreg.workflow.Payloads.RegistrationPayload;
import edu.srmist.eventreg.workflow.Payloads.UnallocatedStudent;
import java.util.List;

/**
 * Input contracts of the job workers, bound with {@code @VariablesAsType}. Only the listed variables are
 * fetched from the engine, which keeps the jobs small and the data contract explicit.
 */
public final class WorkerVariables {

  private WorkerVariables() {}

  public record RecordInvalidInput(RegistrationPayload registration, String invalidReason, List<MemberCheck> memberChecks) {}

  public record SaveApprovedInput(RegistrationPayload registration, String decidedBy, String faRemarks, Boolean escalated) {}

  public record RecordRejectionInput(RegistrationPayload registration, String rejectionRemarks, String decidedBy) {}

  public record RecordNotificationInput(NotificationPayload notification, List<Delivery> deliveries, String deliveryError) {}

  public record FetchUnallocatedInput(Long eventId) {}

  public record AllocateInput(Long eventId, List<UnallocatedStudent> unallocatedStudents, Integer minTeamSize,
                              Integer maxTeamSize, Integer allocationRound) {}
}
