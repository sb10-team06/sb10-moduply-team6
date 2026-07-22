package com.team6.moduply.notification.repository;

import com.team6.moduply.notification.entity.Notification;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Optional<Notification> findByIdAndReceiverId(UUID id, UUID receiverId);

  @Query("""
    SELECT n FROM Notification n
    WHERE n.receiverId = :receiverId
      AND (n.createdAt > :lastCreatedAt
           OR (n.createdAt = :lastCreatedAt AND n.id > :lastId))
    ORDER BY n.createdAt ASC, n.id ASC
    """)
  Slice<Notification> findMissedNotifications(
          @Param("receiverId") UUID receiverId,
          @Param("lastCreatedAt") Instant lastCreatedAt,
          @Param("lastId") UUID lastId,
          Pageable pageable);
}
