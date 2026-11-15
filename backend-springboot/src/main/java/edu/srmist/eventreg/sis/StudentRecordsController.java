package edu.srmist.eventreg.sis;

import edu.srmist.eventreg.config.SimulationSettings;
import edu.srmist.eventreg.domain.EnrollmentStatus;
import edu.srmist.eventreg.domain.Student;
import edu.srmist.eventreg.repository.StudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simulated university Student Records (SIS) REST API. It is called by the Camunda REST Outbound Connector,
 * not by the portal: one call per team member (parallel multi-instance). In production this would be the
 * university's real SIS; only the connector secret STUDENT_RECORDS_URL changes.
 */
@RestController
@RequestMapping("/api/sis")
@Tag(name = "Student Records (simulated SIS)", description = "Called by the REST connector during verification")
public class StudentRecordsController {

  private final StudentRepository students;
  private final SimulationSettings simulation;

  public StudentRecordsController(StudentRepository students, SimulationSettings simulation) {
    this.students = students;
    this.simulation = simulation;
  }

  @GetMapping("/students/{regNo}")
  @Operation(summary = "Enrollment and eligibility of one student (404 if unknown, 503 during a simulated outage)")
  public ResponseEntity<Map<String, Object>> student(@PathVariable String regNo, @RequestParam(required = false) Long eventId) {
    if (simulation.studentRecordsOutage()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("message", "Student Records is temporarily unavailable"));
    }
    Optional<Student> found = students.findById(regNo.trim().toUpperCase());
    if (found.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No student with registration number " + regNo));
    }
    Student s = found.get();
    boolean eligible = s.getEnrollmentStatus() == EnrollmentStatus.ACTIVE;
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("regNo", s.getRegNo());
    body.put("name", s.getName());
    body.put("department", s.getDepartment());
    body.put("yearOfStudy", s.getYearOfStudy());
    body.put("enrollmentStatus", s.getEnrollmentStatus().name());
    body.put("eligible", eligible);
    body.put("reason", switch (s.getEnrollmentStatus()) {
      case ACTIVE -> "Enrolled";
      case SUSPENDED -> "Academic suspension";
      case GRADUATED -> "No longer enrolled (graduated)";
    });
    return ResponseEntity.ok(body);
  }
}
