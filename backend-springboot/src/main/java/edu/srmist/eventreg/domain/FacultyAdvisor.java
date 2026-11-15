package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Faculty Advisor (FA): approves the teams of the students they advise. */
@Entity
@Table(name = "faculty_advisors")
@Getter
@Setter
@NoArgsConstructor
public class FacultyAdvisor {

  /** Also used as the Camunda user task assignee. */
  @Id
  @Column(length = 40)
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  private String department;

  public FacultyAdvisor(String id, String name, String email, String department) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.department = department;
  }
}
