package com.pm.patientservice.repository;

import com.pm.patientservice.model.Patient;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, UUID id);

  /** Case-insensitive match on name or email. {@code term} must be LIKE-escaped. */
  @Query("select p from Patient p "
      + "where lower(p.name) like lower(concat('%', :term, '%')) escape '!' "
      + "or lower(p.email) like lower(concat('%', :term, '%')) escape '!'")
  Page<Patient> search(@Param("term") String term, Pageable pageable);
}
