package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.WorkflowEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, Long> {

  List<WorkflowEvent> findByRegistrationIdOrderByOccurredAtAscIdAsc(String registrationId);

  List<WorkflowEvent> findByEventIdOrderByOccurredAtDescIdDesc(Long eventId);

  List<WorkflowEvent> findTop25ByOrderByOccurredAtDescIdDesc();
}
