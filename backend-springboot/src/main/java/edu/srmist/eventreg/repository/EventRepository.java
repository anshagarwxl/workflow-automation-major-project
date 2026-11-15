package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.Event;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
  Optional<Event> findByCode(String code);

  List<Event> findAllByOrderByRegistrationDeadlineAsc();
}
