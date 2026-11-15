package edu.srmist.eventreg.seed;

import edu.srmist.eventreg.domain.*;
import edu.srmist.eventreg.repository.EventRepository;
import edu.srmist.eventreg.repository.FacultyAdvisorRepository;
import edu.srmist.eventreg.repository.StudentRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds Faculty Advisors, a student directory and three events on an empty database (fictional data). */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
  private static final String DOMAIN = "@campus.example.edu";

  private final FacultyAdvisorRepository advisors;
  private final StudentRepository students;
  private final EventRepository events;

  public DemoDataSeeder(FacultyAdvisorRepository advisors, StudentRepository students, EventRepository events) {
    this.advisors = advisors;
    this.students = students;
    this.events = events;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (events.count() > 0) {
      return;
    }
    FacultyAdvisor priya = advisors.save(new FacultyAdvisor("FA-CSE-01", "Dr. Priya Raman", "priya.raman" + DOMAIN, "CSE"));
    FacultyAdvisor arjun = advisors.save(new FacultyAdvisor("FA-CSE-02", "Dr. Arjun Mehta", "arjun.mehta" + DOMAIN, "CSE"));
    FacultyAdvisor kavitha = advisors.save(new FacultyAdvisor("FA-IT-01", "Dr. Kavitha Srinivasan", "kavitha.s" + DOMAIN, "IT"));
    FacultyAdvisor rahul = advisors.save(new FacultyAdvisor("FA-ECE-01", "Dr. Rahul Verma", "rahul.verma" + DOMAIN, "ECE"));

    Object[][] directory = {
        {"Aarav Sharma", "CSE", 3, "A", priya, EnrollmentStatus.ACTIVE},
        {"Diya Iyer", "CSE", 3, "A", priya, EnrollmentStatus.ACTIVE},
        {"Kabir Nair", "CSE", 3, "A", priya, EnrollmentStatus.ACTIVE},
        {"Ananya Reddy", "CSE", 3, "A", priya, EnrollmentStatus.ACTIVE},
        {"Vihaan Gupta", "CSE", 3, "B", arjun, EnrollmentStatus.ACTIVE},
        {"Ishita Menon", "CSE", 3, "B", arjun, EnrollmentStatus.ACTIVE},
        {"Rohan Das", "CSE", 3, "B", arjun, EnrollmentStatus.ACTIVE},
        {"Meera Pillai", "CSE", 3, "B", arjun, EnrollmentStatus.ACTIVE},
        {"Aditya Rao", "IT", 2, "A", kavitha, EnrollmentStatus.ACTIVE},
        {"Sneha Kulkarni", "IT", 2, "A", kavitha, EnrollmentStatus.ACTIVE},
        {"Arjun Bose", "IT", 2, "A", kavitha, EnrollmentStatus.ACTIVE},
        {"Tara Joshi", "IT", 2, "A", kavitha, EnrollmentStatus.ACTIVE},
        {"Nikhil Varma", "ECE", 3, "C", rahul, EnrollmentStatus.ACTIVE},
        {"Pooja Hegde", "ECE", 3, "C", rahul, EnrollmentStatus.ACTIVE},
        {"Siddharth Jain", "ECE", 3, "C", rahul, EnrollmentStatus.ACTIVE},
        {"Riya Chatterjee", "ECE", 3, "C", rahul, EnrollmentStatus.ACTIVE},
        {"Yash Malhotra", "CSE", 2, "D", priya, EnrollmentStatus.ACTIVE},
        {"Neha Saxena", "CSE", 2, "D", arjun, EnrollmentStatus.ACTIVE},
        {"Kunal Shetty", "IT", 3, "B", kavitha, EnrollmentStatus.ACTIVE},
        {"Lavanya Krishnan", "ECE", 2, "D", rahul, EnrollmentStatus.ACTIVE},
        {"Harsh Agrawal", "CSE", 4, "A", priya, EnrollmentStatus.ACTIVE},
        {"Simran Kaur", "IT", 4, "A", kavitha, EnrollmentStatus.ACTIVE},
        {"Varun Thakur", "CSE", 3, "B", arjun, EnrollmentStatus.SUSPENDED},
        {"Gauri Patil", "ECE", 4, "C", rahul, EnrollmentStatus.GRADUATED},
    };
    int n = 1;
    for (Object[] row : directory) {
      Student s = new Student();
      s.setRegNo(String.format("RA24110030100%02d", n++));
      s.setName((String) row[0]);
      s.setEmail(((String) row[0]).toLowerCase().replace(' ', '.') + DOMAIN);
      s.setDepartment((String) row[1]);
      s.setYearOfStudy((Integer) row[2]);
      s.setSection((String) row[3]);
      s.setFacultyAdvisor((FacultyAdvisor) row[4]);
      s.setEnrollmentStatus((EnrollmentStatus) row[5]);
      students.save(s);
    }

    events.saveAll(List.of(
        event("CODESPRINT-26", "CodeSprint Hackathon 2026",
            "24-hour hackathon. Teams of 3 to 4 build a working prototype for a campus problem statement.",
            "Tech Park, Hall 3", 21, 3, 4),
        event("ROBOWARS-26", "RoboWars Challenge 2026",
            "Design and drive a combat robot. Teams of 2 to 4.", "Mechanical Block Arena", 14, 2, 4),
        event("QUIZBOWL-26", "Tech Quiz Bowl 2026",
            "Inter-department technical quiz. Teams of 2 or 3.", "Main Auditorium", 10, 2, 3)));
    log.info("Seeded {} Faculty Advisors, {} students and 3 events", advisors.count(), students.count());
  }

  private static Event event(String code, String name, String description, String venue, int daysAhead, int min, int max) {
    Event e = new Event();
    e.setCode(code);
    e.setName(name);
    e.setDescription(description);
    e.setVenue(venue);
    e.setEventDate(LocalDate.now().plusDays(daysAhead + 7));
    e.setRegistrationDeadline(OffsetDateTime.now().plusDays(daysAhead).withHour(23).withMinute(59).withSecond(0).withNano(0));
    e.setMinTeamSize(min);
    e.setMaxTeamSize(max);
    e.setMaxAllocationRounds(3);
    e.setCoordinatorName("Event Coordination Cell");
    e.setCoordinatorEmail("coordinator" + DOMAIN);
    return e;
  }
}
