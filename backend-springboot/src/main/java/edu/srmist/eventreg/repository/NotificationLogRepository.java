package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.NotificationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

  List<NotificationLog> findByRegistrationIdOrderBySentAtDesc(String registrationId);

  List<NotificationLog> findTop100ByOrderBySentAtDesc();
}
