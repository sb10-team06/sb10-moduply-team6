package com.team6.moduply.follow.repository;

import com.team6.moduply.follow.entity.Follow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
  // 나와 특정인의 팔로우 관계를 조회
  Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
  // 특정인을 팔로우한 사람의 수.
  // Follow 테이블에 (follower_id, followee_id) 있다고 치면 (?, followee_id)의 COUNT
  long countByFolloweeId(UUID followeeId);

  @Query("""
      select f.follower.id
      from Follow f
      where f.followee.id = :followeeId
      order by f.id
      """)
  List<UUID> findFollowerIdsByFolloweeId(@Param("followeeId") UUID followeeId, Pageable pageable);
}
