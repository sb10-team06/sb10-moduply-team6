package com.team6.moduply.user.repository;

import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.repository.qdsl.UserQDSLRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, UserQDSLRepository {
  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);

  @EntityGraph(attributePaths = "profileImg")
  @Query("select u from User u where u.id in :ids")
  List<User> findAllByIdWithProfileImg(@Param("ids") List<UUID> ids);
}
