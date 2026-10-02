package com.pm.authservice.repository;

import com.pm.authservice.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Page<User> findByRole(String role, Pageable pageable);

  List<User> findByRoleAndEnabledTrueOrderByLastNameAscFirstNameAsc(String role);
}
