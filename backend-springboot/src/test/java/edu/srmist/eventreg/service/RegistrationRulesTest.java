package edu.srmist.eventreg.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.srmist.eventreg.domain.Event;
import edu.srmist.eventreg.domain.EventStatus;
import edu.srmist.eventreg.domain.RegistrationMode;
import edu.srmist.eventreg.domain.RegistrationSource;
import edu.srmist.eventreg.workflow.Payloads.MemberPayload;
import edu.srmist.eventreg.workflow.Payloads.RegistrationPayload;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationRulesTest {

  private final OffsetDateTime now = OffsetDateTime.parse("2026-11-15T10:00:00+05:30");

  private Event event() {
    Event e = new Event();
    e.setName("CodeSprint");
    e.setStatus(EventStatus.OPEN);
    e.setRegistrationDeadline(now.plusDays(3));
    e.setMinTeamSize(3);
    e.setMaxTeamSize(4);
    return e;
  }

  private static MemberPayload m(String regNo) {
    return new MemberPayload(regNo, "Name " + regNo, regNo.toLowerCase() + "@campus.example.edu");
  }

  private static RegistrationPayload team(RegistrationSource source, List<MemberPayload> members, String faId) {
    return new RegistrationPayload("REG-TEST01", 1L, "CodeSprint", RegistrationMode.TEAM, source, "Byte Busters",
        members, faId, "Dr. FA", "fa@campus.example.edu", null);
  }

  @Test
  void validTeamHasNoViolations() {
    assertThat(RegistrationRules.structuralViolations(
        team(RegistrationSource.SELF_FORMED, List.of(m("RA1"), m("RA2"), m("RA3")), "FA-1"), event(), now)).isEmpty();
  }

  @Test
  void detectsTeamSizeDuplicatesAndMissingAdvisor() {
    List<String> v = RegistrationRules.structuralViolations(
        team(RegistrationSource.SELF_FORMED, List.of(m("RA1"), m("RA1")), null), event(), now);
    assertThat(v).anyMatch(s -> s.contains("Team size must be between 3 and 4"));
    assertThat(v).anyMatch(s -> s.contains("listed more than once"));
    assertThat(v).anyMatch(s -> s.contains("Faculty Advisor"));
  }

  @Test
  void rejectsLateSelfFormedTeamButAcceptsRandomTeam() {
    Event closed = event();
    closed.setStatus(EventStatus.CLOSED);
    List<MemberPayload> members = List.of(m("RA1"), m("RA2"), m("RA3"));
    assertThat(RegistrationRules.structuralViolations(team(RegistrationSource.SELF_FORMED, members, "FA-1"), closed, now))
        .anyMatch(s -> s.contains("closed"));
    assertThat(RegistrationRules.structuralViolations(team(RegistrationSource.RANDOM, members, "FA-1"), closed, now))
        .isEmpty();
  }

  @Test
  void individualMustListExactlyOneStudent() {
    RegistrationPayload p = new RegistrationPayload("REG-TEST02", 1L, "CodeSprint", RegistrationMode.INDIVIDUAL,
        RegistrationSource.SELF_FORMED, null, List.of(m("RA1"), m("RA2")), null, null, null, null);
    assertThat(RegistrationRules.structuralViolations(p, event(), now)).anyMatch(s -> s.contains("exactly one"));
  }
}
