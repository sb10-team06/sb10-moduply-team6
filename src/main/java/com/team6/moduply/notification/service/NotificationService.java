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
        .title("플레이리스트 구독 알림")
        .content("'" + playlistTitle + "' 플레이리스트를 누군가 구독했습니다.")
        .level(NotificationLevel.INFO)
        .build();

    notificationRepository.save(notification);
  }

  @Transactional
  public void sendContentAddedNotification(List<UUID> receiverIds, String playlistTitle, String contentTitle) {
    List<Notification> notifications = receiverIds.stream()
        .map(receiverId -> Notification.builder()
            .receiverId(receiverId)
            .type(NotificationType.CONTENT_ADDED)
            .title("새 콘텐츠 추가 알림")
            .content("'" + playlistTitle + "' 플레이리스트에 '" + contentTitle + "' 콘텐츠가 추가되었습니다.")
            .level(NotificationLevel.INFO)
            .build())
        .toList();

    notificationRepository.saveAll(notifications);
  }
}