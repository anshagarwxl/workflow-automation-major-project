package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.PoolEntry;
import edu.srmist.eventreg.domain.PoolStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolEntryRepository extends JpaRepository<PoolEntry, Long> {

  List<PoolEntry> findByEventIdOrderByAddedAtAsc(Long eventId);

  List<PoolEntry> findByEventIdAndStatusOrderByAddedAtAsc(Long eventId, PoolStatus status);

  Optional<PoolEntry> findByEventIdAndRegNo(Long eventId, String regNo);

  List<PoolEntry> findByEventIdAndRegNoIn(Long eventId, Collection<String> regNos);

  long countByEventIdAndStatus(Long eventId, PoolStatus status);

  long countByStatus(PoolStatus status);
}
