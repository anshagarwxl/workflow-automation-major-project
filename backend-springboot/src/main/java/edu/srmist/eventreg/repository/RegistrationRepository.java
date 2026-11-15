package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.Registration;
import edu.srmist.eventreg.domain.RegistrationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<Registration, String> {

  List<Registration> findByEventIdOrderByCreatedAtDesc(Long eventId);

  List<Registration> findAllByOrderByCreatedAtDesc();

  Optional<Registration> findByProcessInstanceKey(Long processInstanceKey);

  List<Registration> findByProcessInstanceKeyIn(Collection<Long> keys);

  long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

  long countByStatus(RegistrationStatus status);

  /** Registrations of an event (other than {@code excludeId}) that list the student and are in one of the statuses. */
  @Query("select distinct r from Registration r join r.members m where r.event.id = :eventId and m.regNo = :regNo "
      + "and r.status in :statuses and r.id <> :excludeId")
  List<Registration> findOtherRegistrationsOfStudent(@Param("eventId") Long eventId, @Param("regNo") String regNo,
      @Param("statuses") Collection<RegistrationStatus> statuses, @Param("excludeId") String excludeId);

  @Query("select distinct r from Registration r join r.members m where m.regNo = :regNo order by r.createdAt desc")
  List<Registration> findByMember(@Param("regNo") String regNo);

  @Query("select count(r) > 0 from Registration r where r.event.id = :eventId and lower(r.teamName) = lower(:teamName) "
      + "and r.status in :statuses and r.id <> :excludeId")
  boolean teamNameTaken(@Param("eventId") Long eventId, @Param("teamName") String teamName,
      @Param("statuses") Collection<RegistrationStatus> statuses, @Param("excludeId") String excludeId);
}
