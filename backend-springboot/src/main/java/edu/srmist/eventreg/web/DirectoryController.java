package edu.srmist.eventreg.web;

import edu.srmist.eventreg.repository.FacultyAdvisorRepository;
import edu.srmist.eventreg.repository.StudentRepository;
import edu.srmist.eventreg.service.EventService;
import edu.srmist.eventreg.service.ViewMapper;
import edu.srmist.eventreg.web.dto.Views.AdvisorView;
import edu.srmist.eventreg.web.dto.Views.ApprovedTeamView;
import edu.srmist.eventreg.web.dto.Views.StudentView;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Directory", description = "Students, Faculty Advisors and approved teams")
public class DirectoryController {

  private final StudentRepository students;
  private final FacultyAdvisorRepository advisors;
  private final EventService events;
  private final ViewMapper views;

  public DirectoryController(StudentRepository students, FacultyAdvisorRepository advisors, EventService events, ViewMapper views) {
    this.students = students;
    this.advisors = advisors;
    this.events = events;
    this.views = views;
  }

  @GetMapping("/students")
  public List<StudentView> searchStudents(@RequestParam(defaultValue = "") String q) {
    return students.search(q.trim(), PageRequest.of(0, 50)).stream().map(views::student).toList();
  }

  @GetMapping("/students/{regNo}")
  public StudentView student(@PathVariable String regNo) {
    return students.findById(regNo.toUpperCase()).map(views::student).orElseThrow(() -> ApiException.notFound("Student " + regNo));
  }

  @GetMapping("/faculty-advisors")
  public List<AdvisorView> advisors() {
    return advisors.findAllByOrderByNameAsc().stream().map(views::advisor).toList();
  }

  @GetMapping("/approved-teams")
  public List<ApprovedTeamView> approvedTeams(@RequestParam(required = false) Long eventId) {
    return events.approvedTeams(eventId);
  }
}
