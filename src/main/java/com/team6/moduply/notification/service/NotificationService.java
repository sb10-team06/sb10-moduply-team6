package com.team6.moduply.notification.service;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.dto.NotificationSearchRequest;
import com.team6.moduply.notification.entity.Notification;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.enums.NotificationType;
import com.team6.moduply.notification.exception.NotificationErrorCode;
import com.team6.moduply.notification.exception.NotificationException;
import com.team6.moduply.notification.mapper.NotificationMapper;
import com.team6.moduply.notification.repository.NotificationRepository;
import com.team6.moduply.notification.repository.qdsl.NotificationQDSLRepository;
import com.team6.moduply.user.enums.Role;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationQDSLRepository notificationQDSLRepository;
  private final NotificationMapper notificationMapper;

  // 알림 읽음 처리
  @Transactional
  public void markAsRead(UUID notificationId, UUID receiverId) {
    Notification notification = notificationRepository
        .findByIdAndReceiverId(notificationId, receiverId)
        .orElseThrow(() -> new NotificationException(
            NotificationErrorCode.NOTIFICATION_NOT_FOUND,
            Map.of("notificationId", notificationId)
        ));

    notification.markAsRead();
  }

  /// 플레이리스트 알림 전송 메서드
  @Transactional
  public NotificationDto sendPlaylistSubscribedNotification(UUID receiverId, String playlistTitle) {
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title(NotificationType.PLAYLIST_SUBSCRIBED.getTitle())
        .content(
            String.format(NotificationType.PLAYLIST_SUBSCRIBED.getMessageTemplate(), playlistTitle))
        .level(NotificationLevel.INFO)
        .build();
    Notification saved = notificationRepository.save(notification);
    return notificationMapper.toDto(saved);
  }

  @Transactional
  public List<NotificationDto> sendContentAddedNotification(List<UUID> receiverIds,
      String playlistTitle, String contentTitle) {
    List<Notification> notifications = receiverIds.stream()
        .map(receiverId -> Notification.builder()
            .receiverId(receiverId)
            .type(NotificationType.CONTENT_ADDED)
            .title(NotificationType.CONTENT_ADDED.getTitle())
            .content(
                String.format(NotificationType.CONTENT_ADDED.getMessageTemplate(), playlistTitle,
                    contentTitle))
            .level(NotificationLevel.INFO)
            .build())
        .toList();
    List<Notification> saved = notificationRepository.saveAll(notifications);
    return saved.stream().map(notificationMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public CursorResponse<NotificationDto> findAll(NotificationSearchRequest request,
      UUID receiverId) {
    List<Notification> notifications = notificationQDSLRepository.findAllWithCursor(request,
        receiverId);
    long totalCount = notificationQDSLRepository.countWithCondition(receiverId);

    boolean hasNext = notifications.size() > request.limit();

    if (hasNext) {
      notifications = notifications.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;

    List<NotificationDto> data = notifications.stream()
        .map(notificationMapper::toDto)
        .toList();

    return new CursorResponse<>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );
  }

  /// 팔로우 알림 전송 메서드.
  @Transactional
  public NotificationDto sendFollowedNotification(UUID receiverId, String followerName) {
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.FOLLOWED)
        .title(NotificationType.FOLLOWED.getTitle())
        .content(String.format(NotificationType.FOLLOWED.getMessageTemplate(), followerName))
        .level(NotificationLevel.INFO)
        .build();
    Notification saved = notificationRepository.save(notification);
    return notificationMapper.toDto(saved);
  }

  @Transactional
  public NotificationDto sendDmReceivedNotification(UUID receiverId, String senderName, String content) {
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.DM_RECEIVED)
        .title(String.format(NotificationType.DM_RECEIVED.getTitle(), senderName))
        .content(String.format(NotificationType.DM_RECEIVED.getMessageTemplate(), content))
        .level(NotificationLevel.INFO)
        .build();
    Notification saved = notificationRepository.save(notification);
    return notificationMapper.toDto(saved);
  }

  @Transactional
  public List<NotificationDto> sendFollowActivityNotification(
      List<UUID> receiverIds,
      String actorName,
      String activityContent
  ) {
    List<Notification> notifications = receiverIds.stream()
        .map(receiverId -> Notification.builder()
            .receiverId(receiverId)
            .type(NotificationType.FOLLOW_ACTIVITY)
            .title(NotificationType.FOLLOW_ACTIVITY.getTitle())
            .content(String.format(
                NotificationType.FOLLOW_ACTIVITY.getMessageTemplate(),
                actorName,
                activityContent
            ))
            .level(NotificationLevel.INFO)
            .build())
        .toList();
    List<Notification> saved = notificationRepository.saveAll(notifications);
    return saved.stream()
        .map(notificationMapper::toDto)
        .toList();
  }

  @Transactional
  public NotificationDto sendRoleUpdatedNotification(UUID receiverId, Role oldRole, Role newRole) {
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.ROLE_CHANGED)
        .title(NotificationType.ROLE_CHANGED.getTitle())
        .content(
            String.format(NotificationType.ROLE_CHANGED.getMessageTemplate(), oldRole, newRole))
        .level(NotificationLevel.INFO)
        .build();
    Notification saved = notificationRepository.save(notification);
    return notificationMapper.toDto(saved);
  }
}
