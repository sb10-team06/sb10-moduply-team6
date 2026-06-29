package com.team6.moduply.follow.repository;

import com.team6.moduply.follow.entity.Follow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
  // 나와 특정인의 팔로우 관계를 조회
  Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}
