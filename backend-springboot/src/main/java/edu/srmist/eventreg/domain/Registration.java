package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/** One registration request (team or individual). Each one runs its own Event Team Registration instance. */
@Entity
@Table(name = "registrations", indexes = {
    @Index(name = "idx_registration_event", columnList = "event_id"),
    @Index(name = "idx_registration_pik", columnList = "processInstanceKey")})
/* Dynamic updates: workers and the task API change different columns of the same row concurrently. */
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class Registration {

  /** Human-readable id, e.g. REG-7F3K9Q. Used as business key in the process variables. */
  @Id
  @Column(length = 20)
  private String id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RegistrationMode mode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RegistrationSource source;

  private String teamName;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "faculty_advisor_id")
  private FacultyAdvisor facultyAdvisor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RegistrationStatus status = RegistrationStatus.SUBMITTED;

  @Column(length = 2000)
  private String statusReason;

  private Long processInstanceKey;

  private Integer allocationRound;

  private String submittedBy;

  private String decision;

  private String decidedBy;

  @Column(length = 2000)
  private String decisionRemarks;

  private OffsetDateTime decidedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(nullable = false)
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @OrderBy("position ASC")
  private List<RegistrationMember> members = new ArrayList<>();

  public void addMember(RegistrationMember member) {
    member.setRegistration(this);
    member.setPosition(members.size());
    members.add(member);
  }

  public void changeStatus(RegistrationStatus newStatus, String reason) {
    this.status = newStatus;
    if (reason != null) {
      this.statusReason = reason;
    }
    this.updatedAt = OffsetDateTime.now();
  }
}
