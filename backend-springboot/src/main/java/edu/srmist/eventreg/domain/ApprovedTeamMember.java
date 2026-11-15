package edu.srmist.eventreg.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedTeamMember {

  @Column(name = "reg_no", nullable = false, length = 30)
  private String regNo;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;
}
