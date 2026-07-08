package com.team6.moduply.user.repository;

import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.repository.qdsl.UserQDSLRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID>, UserQDSLRepository {
  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);
}
