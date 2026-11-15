package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One round of Random Team Allocation for an event. */
@Entity
@Table(name = "allocation_runs")
@Getter
@Setter
@NoArgsConstructor
public class AllocationRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  private int round;

  private int studentsInPool;

  private int teamsCreated;

  private int studentsLeftOver;

  private Long processInstanceKey;

  @Column(nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
