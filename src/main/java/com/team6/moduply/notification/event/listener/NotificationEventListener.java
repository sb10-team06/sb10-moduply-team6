package com.team6.moduply.notification.event.listener;

import com.team6.moduply.common.config.AsyncConfig;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.enums.NotificationType;
import com.team6.moduply.notification.event.ContentAddedEvent;
import com.team6.moduply.notification.event.DirectMessageReceivedEvent;
import com.team6.moduply.notification.event.FollowActivityEvent;
import com.team6.moduply.notification.event.FollowedEvent;
import com.team6.moduply.notification.event.PlaylistSubscribedEvent;
import com.team6.moduply.notification.event.UserRoleUpdatedEvent;
import com.team6.moduply.notification.service.NotificationService;
import com.team6.moduply.follow.repository.FollowRepository;
import com.team6.moduply.playlist.repository.PlaylistSubscriptionRepository;
import com.team6.moduply.sse.SseEmitterManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

  private static final int FOLLOW_ACTIVITY_NOTIFICATION_BATCH_SIZE = 500;

  private final NotificationService notificationService;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final FollowRepository followRepository;
  private final SseEmitterManager sseEmitterManager;

  /// 플레이리스트 관련 알림
  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePlaylistSubscribed(PlaylistSubscribedEvent event) {
    log.info("[알림 저장] 플레이리스트 구독 - playlistId={}", event.getPlaylistId());
    try {
      NotificationDto dto = notificationService.sendPlaylistSubscribedNotification(
          event.getPlaylistOwnerId(),
          event.getPlaylistTitle()
      );
      sseEmitterManager.send(event.getPlaylistOwnerId(), dto);
    } catch (Exception e) {
      log.error("[알림 저장 실패] 플레이리스트 구독 알림 - playlistId={}",
          event.getPlaylistId(), e);
    }
  }

  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleContentAdded(ContentAddedEvent event) {
    log.info("[알림 저장] 콘텐츠 추가 - playlistId={}", event.getPlaylistId());
    try {
      List<UUID> subscriberIds = playlistSubscriptionRepository
          .findSubscriberIdsByPlaylistId(event.getPlaylistId());

      if (!subscriberIds.isEmpty()) {
        List<NotificationDto> dtos = notificationService.sendContentAddedNotification(
            subscriberIds,
            event.getPlaylistTitle(),
            event.getContentTitle()
        );
        for (int i = 0; i < subscriberIds.size(); i++) {
          sseEmitterManager.send(subscriberIds.get(i), dtos.get(i));
        }
      }
    } catch (Exception e) {
      log.error("[알림 저장 실패] 콘텐츠 추가 알림 - playlistId={}",
          event.getPlaylistId(), e);
    }
  }

  /// 팔로우 발생시 발생
  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFollowed(FollowedEvent event) {
    log.info("[알림 저장] 팔로우 - followerId={}, followeeId={}",
        event.getFollowerId(), event.getFolloweeId());
    try {
      NotificationDto dto = notificationService.sendFollowedNotification(
          event.getFolloweeId(),
          event.getFollowerName()
      );

      sseEmitterManager.send(event.getFolloweeId(), dto);
    } catch (Exception e) {
      log.error("[알림 저장 실패] 팔로우 알림 - followerId={}, followeeId={}",
          event.getFollowerId(), event.getFolloweeId(), e);
    }
  }

  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDirectMessageReceived(DirectMessageReceivedEvent event) {
    log.info("[알림 저장] DM 수신 - senderId={}, receiverId={}, conversationId={}",
        event.getSenderId(), event.getReceiverId(), event.getConversationId());
    try {
      NotificationDto dto = notificationService.sendDmReceivedNotification(
          event.getReceiverId(),
          event.getSenderName()
      );
      sseEmitterManager.send(event.getReceiverId(), dto);
    } catch (Exception e) {
      log.error("[알림 저장 실패] DM 수신 알림 - senderId={}, receiverId={}, conversationId={}",
          event.getSenderId(), event.getReceiverId(), event.getConversationId(), e);
    }
  }

  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFollowActivity(FollowActivityEvent event) {
    log.info("[알림 저장] 팔로우 활동 - actorId={}", event.getActorId());
    try {
      int page = 0;
      List<UUID> followerIds;
      do {
        followerIds = followRepository.findFollowerIdsByFolloweeId(
            event.getActorId(),
            // 팔로워 수 많은 계정을 위해 Page 적용
            // 500명씩 잘라서 조회
            PageRequest.of(page, FOLLOW_ACTIVITY_NOTIFICATION_BATCH_SIZE)
        );
        if (!followerIds.isEmpty()) {
          // 500명씩 알림 저장..
          List<NotificationDto> dtos = notificationService.sendFollowActivityNotification(
              followerIds,
              event.getActorName(),
              event.getActivityContent()
          );
          for (NotificationDto dto : dtos) {
            sseEmitterManager.send(dto.receiverId(), dto);
          }
        }
        page++;
      } while (followerIds.size() == FOLLOW_ACTIVITY_NOTIFICATION_BATCH_SIZE);
    } catch (Exception e) {
      log.error("[알림 저장 실패] 팔로우 활동 알림 - actorId={}", event.getActorId(), e);
    }
  }

  @Async(AsyncConfig.NOTIFICATION_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserRoleUpdate(UserRoleUpdatedEvent event) {
    log.info("[알림 저장] 사용자 권한 변경 알림 발송 - receiverId={}, role={}",
        event.getReceiverId(), event.getNewRole());
    NotificationDto dto;
    try {
      dto = notificationService.sendRoleUpdatedNotification(
          event.getReceiverId(),
          event.getOldRole(),
          event.getNewRole()
      );
    } catch (Exception e) {
      log.error("[알림 저장 실패] 사용자 권한 변경 알림 - receiverId={}, role={}",
          event.getReceiverId(), event.getNewRole(), e);
      return;
    }

    try {
      sseEmitterManager.send(event.getReceiverId(), dto);
    } catch (Exception e) {
      log.error("[SSE 전송 실패] 사용자 권한 변경 알림 - receiverId={}, role={}",
          event.getReceiverId(), event.getNewRole(), e);
    }
  }
}
