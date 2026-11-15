package edu.srmist.eventreg.domain;

import java.util.EnumSet;
import java.util.Set;

/** Read-model status of a registration, kept in sync by the job workers and task listeners. */
public enum RegistrationStatus {
  SUBMITTED,
  VERIFYING,
  MANUAL_VERIFICATION,
  INVALID,
  POOLED,
  PENDING_FA_REVIEW,
  ESCALATED,
  APPROVED,
  REJECTED;

  /** Statuses in which a student is considered "taken" for an event. */
  public static final Set<RegistrationStatus> ACTIVE =
      EnumSet.of(SUBMITTED, VERIFYING, MANUAL_VERIFICATION, POOLED, PENDING_FA_REVIEW, ESCALATED, APPROVED);

  public boolean isFinal() {
    return this == INVALID || this == APPROVED || this == REJECTED;
  }
}
