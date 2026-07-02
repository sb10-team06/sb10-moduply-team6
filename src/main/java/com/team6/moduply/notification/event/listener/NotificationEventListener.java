package com.team6.moduply.notification.event.listener;

import com.team6.moduply.notification.event.ContentAddedEvent;
import com.team6.moduply.notification.event.PlaylistSubscribedEvent;
import com.team6.moduply.notification.service.NotificationService;
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

  @Async
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

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleContentAdded(ContentAddedEvent event) {
    log.info("[알림 발송] 콘텐츠 추가 - playlistId: {}", event.getPlaylistId());
    try {
      notificationService.sendContentAddedNotification(
          event.getSubscriberIds(),
          event.getPlaylistTitle(),
          event.getContentTitle()
      );
    } catch (Exception e) {
      log.error("[알림 발송 실패] 콘텐츠 추가 알림 - playlistId: {}",
          event.getPlaylistId(), e);
    }
  }
}
