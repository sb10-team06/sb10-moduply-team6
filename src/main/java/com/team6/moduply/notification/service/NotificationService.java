package com.team6.moduply.notification.service;

import com.team6.moduply.notification.entity.Notification;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.enums.NotificationType;
import com.team6.moduply.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;

  @Transactional
  public void sendPlaylistSubscribedNotification(UUID receiverId, String playlistTitle) {
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title(NotificationType.PLAYLIST_SUBSCRIBED.getTitle())
        .content(String.format(NotificationType.PLAYLIST_SUBSCRIBED.getMessageTemplate(), playlistTitle))
        .level(NotificationLevel.INFO)
        .build();
    notificationRepository.save(notification);
  }

  public void sendContentAddedNotification(List<UUID> receiverIds, String playlistTitle, String contentTitle) {
    List<Notification> notifications = receiverIds.stream()
        .map(receiverId -> Notification.builder()
            .receiverId(receiverId)
            .type(NotificationType.CONTENT_ADDED)
            .title(NotificationType.CONTENT_ADDED.getTitle())
            .content(String.format(NotificationType.CONTENT_ADDED.getMessageTemplate(), playlistTitle, contentTitle))
            .level(NotificationLevel.INFO)
            .build())
        .toList();
    notificationRepository.saveAll(notifications);
  }
}