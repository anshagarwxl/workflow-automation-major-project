package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.ApprovedTeam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovedTeamRepository extends JpaRepository<ApprovedTeam, Long> {

  List<ApprovedTeam> findByEventIdOrderByApprovedAtDesc(Long eventId);

  List<ApprovedTeam> findAllByOrderByApprovedAtDesc();

  Optional<ApprovedTeam> findByRegistrationId(String registrationId);

  long countByEventId(Long eventId);

  @Query("select t from ApprovedTeam t join t.members m where t.event.id = :eventId and m.regNo = :regNo")
  List<ApprovedTeam> findByEventAndMember(@Param("eventId") Long eventId, @Param("regNo") String regNo);
}
