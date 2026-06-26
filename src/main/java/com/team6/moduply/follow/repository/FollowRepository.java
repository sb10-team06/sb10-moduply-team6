package com.team6.moduply.follow.repository;

import com.team6.moduply.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

  /// 팔로우관계가 존재하는지 조회
  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}
