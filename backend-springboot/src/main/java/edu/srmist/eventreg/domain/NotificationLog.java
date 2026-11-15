package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One email sent (or attempted) by the notify-team child process. */
@Entity
@Table(name = "notification_log", indexes = @Index(name = "idx_notification_registration", columnList = "registrationId"))
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 20)
  private String registrationId;

  @Column(nullable = false, length = 40)
  private String template;

  private String recipientRegNo;

  private String recipientEmail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationStatus status;

  @Column(length = 2000)
  private String detail;

  private Long processInstanceKey;

  @Column(nullable = false)
  private OffsetDateTime sentAt = OffsetDateTime.now();
}
