package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** "Approved Teams DB": a team confirmed by its Faculty Advisor (or the Event Coordinator after escalation). */
@Entity
@Table(name = "approved_teams")
@Getter
@Setter
@NoArgsConstructor
public class ApprovedTeam {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String teamCode;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @Column(nullable = false, unique = true, length = 20)
  private String registrationId;

  @Column(nullable = false)
  private String teamName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RegistrationSource source;

  private String facultyAdvisorId;

  private String approvedBy;

  private boolean escalated;

  @Column(length = 2000)
  private String remarks;

  @Column(nullable = false)
  private OffsetDateTime approvedAt = OffsetDateTime.now();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "approved_team_members", joinColumns = @JoinColumn(name = "approved_team_id"))
  private List<ApprovedTeamMember> members = new ArrayList<>();
}
