package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A student waiting in the unallocated pool of an event. */
@Entity
@Table(name = "pool_entries", uniqueConstraints = @UniqueConstraint(name = "uk_pool_event_student", columnNames = {"event_id", "regNo"}))
@Getter
@Setter
@NoArgsConstructor
public class PoolEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @Column(nullable = false, length = 30)
  private String regNo;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  /** The student's own Faculty Advisor, used to pick the FA of a random team. */
  private String facultyAdvisorId;

  /** The individual registration that put the student into the pool. */
  private String sourceRegistrationId;

  /** The random-team registration the student is currently allocated to. */
  private String allocatedRegistrationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PoolStatus status = PoolStatus.WAITING;

  private int timesAllocated;

  @Column(nullable = false)
  private OffsetDateTime addedAt = OffsetDateTime.now();

  private OffsetDateTime updatedAt = OffsetDateTime.now();

  public void moveTo(PoolStatus newStatus, String registrationId) {
    this.status = newStatus;
    this.allocatedRegistrationId = registrationId;
    this.updatedAt = OffsetDateTime.now();
  }
}
