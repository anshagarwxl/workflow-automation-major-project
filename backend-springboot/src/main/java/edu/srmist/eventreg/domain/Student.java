package edu.srmist.eventreg.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Student directory entry, the data behind the simulated Student Records (SIS) REST service. */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

  @Id
  @Column(name = "reg_no", length = 30)
  private String regNo;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  private String department;

  private int yearOfStudy;

  private String section;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EnrollmentStatus enrollmentStatus = EnrollmentStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "faculty_advisor_id")
  private FacultyAdvisor facultyAdvisor;
}
