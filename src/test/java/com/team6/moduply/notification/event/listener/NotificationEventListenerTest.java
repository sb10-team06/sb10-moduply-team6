package com.team6.moduply.notification.event.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.common.redis.RedisPublisher;
import com.team6.moduply.follow.repository.FollowRepository;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.event.ContentAddedEvent;
import com.team6.moduply.notification.event.DirectMessageReceivedEvent;
import com.team6.moduply.notification.event.FollowActivityEvent;
import com.team6.moduply.notification.event.FollowedEvent;
import com.team6.moduply.notification.event.PlaylistSubscribedEvent;
import com.team6.moduply.notification.event.UserRoleUpdatedEvent;
import com.team6.moduply.notification.service.NotificationService;
import com.team6.moduply.playlist.repository.PlaylistSubscriptionRepository;
import com.team6.moduply.user.enums.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  private static final String SSE_CHANNEL_PREFIX = "sse:notification:";

  @InjectMocks
  private NotificationEventListener listener;

  @Mock
  private NotificationService notificationService;

  @Mock
  private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Mock
  private FollowRepository followRepository;

  @Mock
  private RedisPublisher redisPublisher;

  @Test
  @DisplayName("권한 변경 이벤트를 받으면 알림을 저장하고 SSE로 전송한다.")
  void handleUserRoleUpdate_success() {
    UUID receiverId = UUID.randomUUID();
    UserRoleUpdatedEvent event =
            new UserRoleUpdatedEvent(receiverId, Role.USER, Role.ADMIN);
    NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            null,
            receiverId,
            "권한 변경 알림",
            "내 권한이 [USER]에서 [ADMIN]로 변경되었어요",
            NotificationLevel.INFO
    );
    given(notificationService.sendRoleUpdatedNotification(
            receiverId, Role.USER, Role.ADMIN)).willReturn(dto);

    listener.handleUserRoleUpdate(event);

    verify(notificationService).sendRoleUpdatedNotification(
            receiverId, Role.USER, Role.ADMIN);
    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + receiverId, dto);
  }

  @Test
  @DisplayName("권한 변경 알림 저장에 실패하면 SSE를 전송하지 않는다.")
  void handleUserRoleUpdate_skip_sse_when_save_fails() {
    UUID receiverId = UUID.randomUUID();
    UserRoleUpdatedEvent event =
            new UserRoleUpdatedEvent(receiverId, Role.USER, Role.ADMIN);
    given(notificationService.sendRoleUpdatedNotification(
            receiverId, Role.USER, Role.ADMIN)).willThrow(new RuntimeException("save failed"));

    assertThatCode(() -> listener.handleUserRoleUpdate(event))
            .doesNotThrowAnyException();

    verify(redisPublisher, never()).publish(any(), any());
  }

  @Test
  @DisplayName("권한 변경 SSE 전송 실패는 저장된 알림에 영향을 주지 않는다.")
  void handleUserRoleUpdate_keep_saved_notification_when_sse_fails() {
    UUID receiverId = UUID.randomUUID();
    UserRoleUpdatedEvent event =
            new UserRoleUpdatedEvent(receiverId, Role.USER, Role.ADMIN);
    NotificationDto dto = new NotificationDto(
            UUID.randomUUID(),
            null,
            receiverId,
            "권한 변경 알림",
            "내 권한이 [USER]에서 [ADMIN]로 변경되었어요",
            NotificationLevel.INFO
    );
    given(notificationService.sendRoleUpdatedNotification(
            receiverId, Role.USER, Role.ADMIN)).willReturn(dto);
    willThrow(new RuntimeException("sse failed"))
            .given(redisPublisher).publish(SSE_CHANNEL_PREFIX + receiverId, dto);

    assertThatCode(() -> listener.handleUserRoleUpdate(event))
            .doesNotThrowAnyException();

    verify(notificationService).sendRoleUpdatedNotification(
            receiverId, Role.USER, Role.ADMIN);
    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + receiverId, dto);
  }

  @Test
  @DisplayName("플레이리스트 구독 이벤트를 받으면 알림을 저장하고 Redis로 전송한다.")
  void handlePlaylistSubscribed_success() {
    UUID ownerId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    PlaylistSubscribedEvent event = new PlaylistSubscribedEvent(
        ownerId, subscriberId, playlistId, "테스터", "테스트 플레이리스트");
    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(), null, ownerId, "구독 알림", "내용", NotificationLevel.INFO);

    given(notificationService.sendPlaylistSubscribedNotification(
        ownerId, "테스터", "테스트 플레이리스트")).willReturn(dto);

    listener.handlePlaylistSubscribed(event);

    verify(notificationService).sendPlaylistSubscribedNotification(ownerId, "테스터", "테스트 플레이리스트");
    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + ownerId, dto);
  }

  @Test
  @DisplayName("팔로우 이벤트를 받으면 알림을 저장하고 Redis로 전송한다.")
  void handleFollowed_success() {
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowedEvent event = new FollowedEvent(followerId, "팔로워", followeeId);
    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(), null, followeeId, "팔로우 알림", "내용", NotificationLevel.INFO);

    given(notificationService.sendFollowedNotification(followeeId, "팔로워")).willReturn(dto);

    listener.handleFollowed(event);

    verify(notificationService).sendFollowedNotification(followeeId, "팔로워");
    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + followeeId, dto);
  }

  @Test
  @DisplayName("DM 수신 이벤트를 받으면 알림을 저장하고 Redis로 전송한다.")
  void handleDirectMessageReceived_success() {
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    DirectMessageReceivedEvent event = new DirectMessageReceivedEvent(
        receiverId, senderId, "발신자", conversationId, "안녕하세요");
    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(), null, receiverId, "DM 알림", "내용", NotificationLevel.INFO);

    given(notificationService.sendDmReceivedNotification(receiverId, "발신자", "안녕하세요")).willReturn(dto);

    listener.handleDirectMessageReceived(event);

    verify(notificationService).sendDmReceivedNotification(receiverId, "발신자", "안녕하세요");
    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + receiverId, dto);
  }

  @Test
  @DisplayName("콘텐츠 추가 이벤트를 받으면 구독자들에게 Redis로 전송한다.")
  void handleContentAdded_success() {
    UUID playlistId = UUID.randomUUID();
    UUID subscriber1 = UUID.randomUUID();
    UUID subscriber2 = UUID.randomUUID();
    ContentAddedEvent event = new ContentAddedEvent(playlistId, "플레이리스트", "콘텐츠");

    given(playlistSubscriptionRepository.findSubscriberIdsByPlaylistId(playlistId))
        .willReturn(List.of(subscriber1, subscriber2));

    NotificationDto dto1 = new NotificationDto(UUID.randomUUID(), null, subscriber1, "제목", "내용", NotificationLevel.INFO);
    NotificationDto dto2 = new NotificationDto(UUID.randomUUID(), null, subscriber2, "제목", "내용", NotificationLevel.INFO);

    given(notificationService.sendContentAddedNotification(
        List.of(subscriber1, subscriber2), "플레이리스트", "콘텐츠"))
        .willReturn(List.of(dto1, dto2));

    listener.handleContentAdded(event);

    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + subscriber1, dto1);
    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + subscriber2, dto2);
  }

  @Test
  @DisplayName("플레이리스트 구독 알림 저장 실패 시 예외가 전파되지 않는다.")
  void handlePlaylistSubscribed_fail() {
    UUID ownerId = UUID.randomUUID();
    PlaylistSubscribedEvent event = new PlaylistSubscribedEvent(
        ownerId, UUID.randomUUID(), UUID.randomUUID(), "플레이리스트", "테스터");

    given(notificationService.sendPlaylistSubscribedNotification(any(), any(), any()))
        .willThrow(new RuntimeException("저장 실패"));

    assertThatCode(() -> listener.handlePlaylistSubscribed(event))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("팔로우 알림 저장 실패 시 예외가 전파되지 않는다.")
  void handleFollowed_fail() {
    FollowedEvent event = new FollowedEvent(UUID.randomUUID(), "팔로워", UUID.randomUUID());

    given(notificationService.sendFollowedNotification(any(), any()))
        .willThrow(new RuntimeException("저장 실패"));

    assertThatCode(() -> listener.handleFollowed(event))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("DM 수신 알림 저장 실패 시 예외가 전파되지 않는다.")
  void handleDirectMessageReceived_fail() {
    DirectMessageReceivedEvent event = new DirectMessageReceivedEvent(
        UUID.randomUUID(), UUID.randomUUID(), "발신자", UUID.randomUUID(), "안녕");

    given(notificationService.sendDmReceivedNotification(any(), any(), any()))
        .willThrow(new RuntimeException("저장 실패"));

    assertThatCode(() -> listener.handleDirectMessageReceived(event))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("팔로우 활동 알림을 팔로워들에게 Redis로 전송한다.")
  void handleFollowActivity_success() {
    UUID actorId = UUID.randomUUID();
    UUID follower1 = UUID.randomUUID();
    FollowActivityEvent event = new FollowActivityEvent(actorId, "액터", "새 리뷰를 작성했습니다.");

    given(followRepository.findFollowerIdsByFolloweeId(eq(actorId), any()))
        .willReturn(List.of(follower1));

    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(), null, follower1, "팔로우 활동", "내용", NotificationLevel.INFO);

    given(notificationService.sendFollowActivityNotification(
        List.of(follower1), "액터", "새 리뷰를 작성했습니다.")).willReturn(List.of(dto));

    listener.handleFollowActivity(event);

    verify(redisPublisher).publish(SSE_CHANNEL_PREFIX + follower1, dto);
  }
}
