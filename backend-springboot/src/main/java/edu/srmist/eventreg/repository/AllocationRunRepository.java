package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.AllocationRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRunRepository extends JpaRepository<AllocationRun, Long> {
  List<AllocationRun> findByEventIdOrderByRoundAsc(Long eventId);
}
