package edu.srmist.eventreg.repository;

import edu.srmist.eventreg.domain.Student;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, String> {

  @Query("select s from Student s where lower(s.regNo) like lower(concat('%', :q, '%')) "
      + "or lower(s.name) like lower(concat('%', :q, '%')) order by s.regNo")
  List<Student> search(@Param("q") String query, Pageable pageable);
}
