package edu.srmist.eventreg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.srmist.eventreg.workflow.Payloads.UnallocatedStudent;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AllocationPlannerTest {

  private static List<UnallocatedStudent> pool(int n) {
    return IntStream.rangeClosed(1, n)
        .mapToObj(i -> new UnallocatedStudent("RA" + i, "Student " + i, "s" + i + "@x.edu", "FA-" + (i % 3)))
        .toList();
  }

  @ParameterizedTest(name = "{0} students, sizes {1}..{2} -> {3} teams, {4} left over")
  @CsvSource({
      "10, 3, 4, 3, 0",   // 4+3+3
      "8, 3, 4, 2, 0",    // 4+4
      "7, 2, 3, 3, 0",    // 3+2+2
      "5, 3, 4, 1, 1",    // no valid split: one team of 4, one left
      "3, 3, 5, 1, 0",
      "13, 3, 4, 4, 0"})  // 4+3+3+3
  void splitsPoolWithinBounds(int n, int min, int max, int expectedTeams, int expectedLeftOver) {
    AllocationPlanner.Plan plan = AllocationPlanner.plan(pool(n), min, max, new Random(42));

    assertThat(plan.teams()).hasSize(expectedTeams);
    assertThat(plan.leftOver()).hasSize(expectedLeftOver);
    assertThat(plan.teams()).allSatisfy(team -> assertThat(team.size()).isBetween(min, max));
    int sizes = plan.teams().stream().mapToInt(List::size).sum();
    assertThat(sizes + plan.leftOver().size()).isEqualTo(n);
  }

  @Test
  void everyStudentIsPlacedExactlyOnce() {
    AllocationPlanner.Plan plan = AllocationPlanner.plan(pool(17), 3, 4, new Random(7));
    Set<String> seen = new HashSet<>();
    plan.teams().forEach(t -> t.forEach(s -> assertThat(seen.add(s.regNo())).isTrue()));
    plan.leftOver().forEach(s -> assertThat(seen.add(s.regNo())).isTrue());
    assertThat(seen).hasSize(17);
  }

  @Test
  void tooFewStudentsRaisesBusinessError() {
    assertThatThrownBy(() -> AllocationPlanner.plan(pool(2), 3, 4, new Random()))
        .isInstanceOf(AllocationPlanner.InsufficientStudentsException.class)
        .hasMessageContaining("at least 3");
  }
}
