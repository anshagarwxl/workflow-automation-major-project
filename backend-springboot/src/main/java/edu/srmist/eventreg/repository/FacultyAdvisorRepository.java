package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.FacultyAdvisor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyAdvisorRepository extends JpaRepository<FacultyAdvisor, String> {
  List<FacultyAdvisor> findAllByOrderByNameAsc();
}
