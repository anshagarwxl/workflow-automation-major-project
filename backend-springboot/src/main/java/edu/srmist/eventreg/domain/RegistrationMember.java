package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A student listed on a registration. */
@Entity
@Table(name = "registration_members", indexes = @Index(name = "idx_member_regno", columnList = "regNo"))
@Getter
@Setter
@NoArgsConstructor
public class RegistrationMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "registration_id")
  private Registration registration;

  private int position;

  @Column(nullable = false, length = 30)
  private String regNo;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  public RegistrationMember(String regNo, String name, String email) {
    this.regNo = regNo;
    this.name = name;
    this.email = email;
  }
}
