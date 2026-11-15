package edu.srmist.eventreg.service;

import edu.srmist.eventreg.domain.Event;
import edu.srmist.eventreg.domain.EventStatus;
import edu.srmist.eventreg.domain.RegistrationMode;
import edu.srmist.eventreg.domain.RegistrationSource;
import edu.srmist.eventreg.workflow.Payloads.MemberPayload;
import edu.srmist.eventreg.workflow.Payloads.RegistrationPayload;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stateless business rules for a registration; database-backed checks live in the workflow service. */
public final class RegistrationRules {

  private RegistrationRules() {}

  public static List<String> structuralViolations(RegistrationPayload r, Event event, OffsetDateTime now) {
    List<String> violations = new ArrayList<>();
    List<MemberPayload> members = r.members() == null ? List.of() : r.members();

    if (r.source() != RegistrationSource.RANDOM) {
      if (event.getStatus() != EventStatus.OPEN) {
        violations.add("Registration for " + event.getName() + " is closed");
      } else if (now.isAfter(event.getRegistrationDeadline())) {
        violations.add("The registration deadline (" + event.getRegistrationDeadline().toLocalDate() + ") has passed");
      }
    }

    if (r.mode() == RegistrationMode.INDIVIDUAL) {
      if (members.size() != 1) {
        violations.add("An individual registration must list exactly one student");
      }
    } else {
      if (r.teamName() == null || r.teamName().isBlank()) {
        violations.add("A team name is required");
      }
      if (members.size() < event.getMinTeamSize() || members.size() > event.getMaxTeamSize()) {
        violations.add("Team size must be between " + event.getMinTeamSize() + " and " + event.getMaxTeamSize()
            + " (got " + members.size() + ")");
      }
      if (r.faId() == null || r.faId().isBlank()) {
        violations.add("A Faculty Advisor must be selected for the team");
      }
    }

    Set<String> seen = new HashSet<>();
    for (MemberPayload m : members) {
      if (m.regNo() == null || m.regNo().isBlank()) {
        violations.add("Every member needs a registration number");
      } else if (!seen.add(m.regNo().trim().toUpperCase())) {
        violations.add("Student " + m.regNo() + " is listed more than once");
      }
      if (m.email() == null || !m.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
        violations.add("Member " + m.regNo() + " has an invalid email address");
      }
    }
    return violations;
  }
}
