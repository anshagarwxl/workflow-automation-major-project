package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/** An event students register for, with its team-size rules and registration window. */
@Entity
@Table(name = "events")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(length = 2000)
  private String description;

  private String venue;

  private LocalDate eventDate;

  @Column(nullable = false)
  private OffsetDateTime registrationDeadline;

  @Column(nullable = false)
  private int minTeamSize;

  @Column(nullable = false)
  private int maxTeamSize;

  @Column(nullable = false)
  private int maxAllocationRounds = 3;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EventStatus status = EventStatus.OPEN;

  private String coordinatorName;

  private String coordinatorEmail;

  /** Key of the Random Team Allocation process instance, set once registration closes. */
  private Long allocationProcessInstanceKey;

  private OffsetDateTime closedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
