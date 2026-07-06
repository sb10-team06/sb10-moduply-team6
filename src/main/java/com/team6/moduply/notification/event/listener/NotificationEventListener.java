package com.team6.moduply.notification.event.listener;

import com.team6.moduply.common.config.AsyncConfig;
import com.team6.moduply.notification.event.ContentAddedEvent;
import com.team6.moduply.notification.event.PlaylistSubscribedEvent;
import com.team6.moduply.notification.service.NotificationService;
import com.team6.moduply.playlist.repository.PlaylistSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePlaylistSubscribed(PlaylistSubscribedEvent event) {
    log.info("[알림 발송] 플레이리스트 구독 - playlistId: {}", event.getPlaylistId());
    try {
      notificationService.sendPlaylistSubscribedNotification(
          event.getPlaylistOwnerId(),
          event.getPlaylistTitle()
      );
    } catch (Exception e) {
      log.error("[알림 발송 실패] 플레이리스트 구독 알림 - playlistId: {}",
          event.getPlaylistId(), e);
    }
  }

  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleContentAdded(ContentAddedEvent event) {
    log.info("[알림 발송] 콘텐츠 추가 - playlistId: {}", event.getPlaylistId());
    try {
      List<UUID> subscriberIds = playlistSubscriptionRepository
          .findSubscriberIdsByPlaylistId(event.getPlaylistId());


      if (!subscriberIds.isEmpty()) {
        notificationService.sendContentAddedNotification(
            subscriberIds,
            event.getPlaylistTitle(),
            event.getContentTitle()
        );
      }
    } catch (Exception e) {
      log.error("[알림 발송 실패] 콘텐츠 추가 알림 - playlistId: {}",
          event.getPlaylistId(), e);
    }
  }
}
