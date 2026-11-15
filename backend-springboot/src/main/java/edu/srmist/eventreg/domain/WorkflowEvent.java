package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audit trail entry written by job workers, task listeners and the REST layer; drives the tracker timeline. */
@Entity
@Table(name = "workflow_events", indexes = {
    @Index(name = "idx_wfe_registration", columnList = "registrationId"),
    @Index(name = "idx_wfe_event", columnList = "eventId")})
@Getter
@Setter
@NoArgsConstructor
public class WorkflowEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 20)
  private String registrationId;

  private Long eventId;

  private Long processInstanceKey;

  /** Short machine-readable step, e.g. VALIDATED, FA_REVIEW_CREATED. */
  @Column(nullable = false, length = 40)
  private String step;

  @Column(nullable = false)
  private String title;

  @Column(length = 2000)
  private String detail;

  /** SYSTEM, STUDENT, FACULTY_ADVISOR, EVENT_COORDINATOR or a user id. */
  @Column(length = 60)
  private String actor;

  /** info, success, warning or error: used for colouring in the UI. */
  @Column(length = 10)
  private String level = "info";

  @Column(nullable = false)
  private OffsetDateTime occurredAt = OffsetDateTime.now();
}
