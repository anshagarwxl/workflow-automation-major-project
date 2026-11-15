package edu.srmist.eventreg.web.dto;

import edu.srmist.eventreg.domain.RegistrationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Request bodies of the REST API (validated with Jakarta Bean Validation). */
public final class Requests {

  private Requests() {}

  public record MemberRequest(
      @NotBlank @Size(max = 30) String regNo,
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email String email) {}

  public record CreateRegistrationRequest(
      @NotNull Long eventId,
      @NotNull RegistrationMode mode,
      @Size(max = 80) String teamName,
      String facultyAdvisorId,
      @NotEmpty @Size(max = 10) List<@Valid MemberRequest> members,
      String submittedBy) {}

  public record CreateEventRequest(
      @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9-]+$", message = "use upper-case letters, digits and dashes") String code,
      @NotBlank String name,
      String description,
      String venue,
      LocalDate eventDate,
      @NotNull @Future OffsetDateTime registrationDeadline,
      @Min(1) @Max(10) int minTeamSize,
      @Min(1) @Max(10) int maxTeamSize,
      @Min(1) @Max(10) Integer maxAllocationRounds,
      String coordinatorName,
      @Email String coordinatorEmail) {}

  public record AssignTaskRequest(@NotBlank String assignee) {}

  public record CompleteTaskRequest(
      @NotBlank String actorId,
      @Pattern(regexp = "APPROVED|REJECTED") String decision,
      @Size(max = 1000) String remarks,
      Boolean manuallyVerified,
      @Size(max = 1000) String invalidReason) {}

  public record SettingsRequest(String slaPreset, Boolean studentRecordsOutage, Boolean forceMemberConflict) {}
}
