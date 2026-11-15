package edu.srmist.eventreg.workflow;

/** Names shared between the BPMN models and the Java code (the data contract). */
public final class ProcessConstants {

  private ProcessConstants() {}

  // Process ids
  public static final String PROCESS_REGISTRATION = "event-team-registration";
  public static final String PROCESS_ALLOCATION = "random-team-allocation";
  public static final String PROCESS_NOTIFY_TEAM = "notify-team";

  // Messages
  public static final String MESSAGE_REGISTRATION_CLOSED = "registration-closed";

  // Job types (service tasks and listeners)
  public static final String JOB_VALIDATE_REGISTRATION = "validate-registration";
  public static final String JOB_RECORD_INVALID = "record-invalid-registration";
  public static final String JOB_ADD_TO_POOL = "add-to-unallocated-pool";
  public static final String JOB_SAVE_APPROVED_TEAM = "save-approved-team";
  public static final String JOB_RECORD_REJECTION = "record-rejection";
  public static final String JOB_RECORD_NOTIFICATION = "record-notification";
  public static final String JOB_FETCH_UNALLOCATED = "fetch-unallocated-students";
  public static final String JOB_ALLOCATE_RANDOM_TEAMS = "allocate-random-teams";
  public static final String JOB_USER_TASK_LIFECYCLE = "user-task-lifecycle";

  // BPMN error codes
  public static final String ERROR_REGISTRATION_INVALID = "REGISTRATION_INVALID";
  public static final String ERROR_MEMBER_CONFLICT = "MEMBER_CONFLICT";
  public static final String ERROR_INSUFFICIENT_STUDENTS = "INSUFFICIENT_STUDENTS";

  // User task element ids
  public static final String TASK_FA_REVIEW = "Task_FAReview";
  public static final String TASK_COORDINATOR_REVIEW = "Task_CoordinatorReview";
  public static final String TASK_MANUAL_VERIFICATION = "Task_ManualVerification";

  // Candidate groups
  public static final String GROUP_FACULTY_ADVISORS = "faculty-advisors";
  public static final String GROUP_EVENT_COORDINATORS = "event-coordinators";

  // Decisions
  public static final String DECISION_APPROVED = "APPROVED";
  public static final String DECISION_REJECTED = "REJECTED";
}
