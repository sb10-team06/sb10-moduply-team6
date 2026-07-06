package com.team6.moduply.user.repository;

import com.team6.moduply.user.entity.SocialAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
  boolean existsByUserId(UUID userId);
  Optional<SocialAccount> findByUserId(UUID userId);
}
