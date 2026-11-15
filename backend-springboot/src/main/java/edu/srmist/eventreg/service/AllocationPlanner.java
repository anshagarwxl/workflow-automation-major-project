package edu.srmist.eventreg.service;

import edu.srmist.eventreg.workflow.Payloads.UnallocatedStudent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure allocation algorithm used by the allocate-random-teams worker.
 *
 * <p>Shuffles the pool and splits it into the fewest teams whose sizes all lie in [min, max], with sizes
 * differing by at most one. If no such split exists (e.g. 5 students, sizes 3..4) it forms as many full
 * teams as possible and leaves the rest in the pool for the next round or for manual placement.
 */
public final class AllocationPlanner {

  public record Plan(List<List<UnallocatedStudent>> teams, List<UnallocatedStudent> leftOver) {}

  public static class InsufficientStudentsException extends RuntimeException {
    public InsufficientStudentsException(String message) {
      super(message);
    }
  }

  private AllocationPlanner() {}

  public static Plan plan(List<UnallocatedStudent> pool, int minSize, int maxSize, Random random) {
    if (minSize < 1 || maxSize < minSize) {
      throw new IllegalArgumentException("Invalid team size range " + minSize + ".." + maxSize);
    }
    int n = pool.size();
    if (n < minSize) {
      throw new InsufficientStudentsException(
          n + " student(s) left in the pool, but a team needs at least " + minSize + " members");
    }
    List<UnallocatedStudent> shuffled = new ArrayList<>(pool);
    Collections.shuffle(shuffled, random);

    int fewestTeams = (n + maxSize - 1) / maxSize; // ceil(n / max)
    int mostTeams = n / minSize;                    // floor(n / min)
    List<List<UnallocatedStudent>> teams = new ArrayList<>();
    if (fewestTeams <= mostTeams) {
      // Even split: every team gets base or base + 1 members, all within bounds
      int base = n / fewestTeams;
      int bigger = n % fewestTeams;
      int index = 0;
      for (int t = 0; t < fewestTeams; t++) {
        int size = base + (t < bigger ? 1 : 0);
        teams.add(new ArrayList<>(shuffled.subList(index, index + size)));
        index += size;
      }
      return new Plan(teams, List.of());
    }
    // No valid even split: fill full teams of maxSize, leave the remainder
    int fullTeams = n / maxSize;
    int index = 0;
    for (int t = 0; t < fullTeams; t++) {
      teams.add(new ArrayList<>(shuffled.subList(index, index + maxSize)));
      index += maxSize;
    }
    return new Plan(teams, new ArrayList<>(shuffled.subList(index, n)));
  }
}
