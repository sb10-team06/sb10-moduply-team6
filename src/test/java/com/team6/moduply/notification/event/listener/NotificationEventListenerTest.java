package com.team6.moduply.notification.event.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.follow.repository.FollowRepository;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.event.UserRoleUpdatedEvent;
import com.team6.moduply.notification.service.NotificationService;
import com.team6.moduply.playlist.repository.PlaylistSubscriptionRepository;
import com.team6.moduply.sse.SseEmitterManager;
import com.team6.moduply.user.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @InjectMocks
  private NotificationEventListener listener;

  @Mock
  private NotificationService notificationService;

  @Mock
  private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Mock
  private FollowRepository followRepository;

  @Mock
  private SseEmitterManager sseEmitterManager;

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
    verify(sseEmitterManager).send(receiverId, dto);
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

    verify(sseEmitterManager, never()).send(any(), any());
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
        .given(sseEmitterManager).send(receiverId, dto);

    assertThatCode(() -> listener.handleUserRoleUpdate(event))
        .doesNotThrowAnyException();

    verify(notificationService).sendRoleUpdatedNotification(
        receiverId, Role.USER, Role.ADMIN);
  }
}
