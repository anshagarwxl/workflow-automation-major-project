package edu.srmist.eventreg.workflow;

import edu.srmist.eventreg.domain.RegistrationMode;
import edu.srmist.eventreg.domain.RegistrationSource;
import java.util.List;

/**
 * Strongly typed process variables. Every record maps 1:1 to a JSON variable in the BPMN models, so the
 * job workers never handle raw maps.
 */
public final class Payloads {

  private Payloads() {}

  /** registration.members[] */
  public record MemberPayload(String regNo, String name, String email) {}

  /** The {@code registration} variable: start payload of Event Team Registration. */
  public record RegistrationPayload(
      String registrationId,
      Long eventId,
      String eventName,
      RegistrationMode mode,
      RegistrationSource source,
      String teamName,
      List<MemberPayload> members,
      String faId,
      String faName,
      String faEmail,
      Integer allocationRound) {}

  /** The {@code sla} variable: ISO-8601 durations read by the timer events through FEEL. */
  public record SlaSettings(String verificationTimeout, String faReminderAfter, String faEscalationAfter) {}

  /** One element of {@code memberChecks}, produced by the REST connector's result expression. */
  public record MemberCheck(String regNo, String name, String department, String enrollmentStatus, Boolean eligible,
                            String reason) {}

  /** The {@code notification} input of the notify-team child process. */
  public record NotificationPayload(String template, String registrationId, String eventName, String teamName,
                                    String teamCode, String source, String remarks, List<MemberPayload> recipients) {}

  /** One element of {@code deliveries}, the output collection of the sequential email multi-instance. */
  public record Delivery(String regNo, String email, String status) {}

  /** One element of {@code unallocatedStudents}. */
  public record UnallocatedStudent(String regNo, String name, String email, String faId) {}

  /** The {@code approvedTeam} variable written by save-approved-team. */
  public record ApprovedTeamRef(String teamCode, Long approvedTeamId, String approvedAt) {}

  // ---- Worker results (returned objects become process variables)

  public record ValidationResult(boolean valid, int memberCount) {}

  public record InvalidRecorded(String invalidReason) {}

  public record PoolResult(boolean pooled, int poolSize) {}

  public record SaveResult(ApprovedTeamRef approvedTeam) {}

  public record RejectionResult(String rejectionRemarks, boolean returnedToPool) {}

  public record NotificationResult(String notificationStatus, int recipients) {}

  public record FetchResult(int unallocatedCount, List<UnallocatedStudent> unallocatedStudents) {}

  public record AllocationResult(List<RegistrationPayload> generatedTeams, int leftOver) {}
}
