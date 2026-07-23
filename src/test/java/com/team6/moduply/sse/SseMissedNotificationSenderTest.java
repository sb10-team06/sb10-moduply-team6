package com.team6.moduply.sse;

import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseMissedNotificationSenderTest {

  @InjectMocks
  private SseMissedNotificationSender missedNotificationSender;

  @Mock
  private NotificationService notificationService;

  @Test
  @DisplayName("lastEventId가 null이면 재전송하지 않는다.")
  void send_skip_when_last_event_id_is_null() {
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = mock(SseEmitter.class);

    missedNotificationSender.send(userId, null, emitter);

    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("잘못된 형식의 lastEventId이면 재전송하지 않는다.")
  void send_skip_when_last_event_id_is_invalid() {
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = mock(SseEmitter.class);

    missedNotificationSender.send(userId, "invalid-format", emitter);

    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("유실된 알림을 재전송한다.")
  void send_missed_notifications() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    String lastEventId = createdAt + ":" + notificationId;

    NotificationDto dto = new NotificationDto(
        notificationId, createdAt, userId, "제목", "내용", NotificationLevel.INFO);

    given(notificationService.findMissedNotifications(eq(userId), any(), any()))
        .willReturn(List.of(dto));

    SseEmitter emitter = mock(SseEmitter.class);

    missedNotificationSender.send(userId, lastEventId, emitter);

    verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  @DisplayName("유실 알림 재전송 실패 시 루프를 중단한다.")
  void send_stops_on_failure() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    String lastEventId = createdAt + ":" + notificationId;

    NotificationDto dto = new NotificationDto(
        notificationId, createdAt, userId, "제목", "내용", NotificationLevel.INFO);

    given(notificationService.findMissedNotifications(eq(userId), any(), any()))
        .willReturn(List.of(dto));

    SseEmitter emitter = mock(SseEmitter.class);
    doThrow(new IOException("전송 실패")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

    missedNotificationSender.send(userId, lastEventId, emitter);

    verify(emitter).completeWithError(any());
  }
}
