package com.team6.moduply.sse;

import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.enums.NotificationLevel;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SseEmitterManagerTest {

  @Mock
  private SseMissedNotificationSender missedNotificationSender;

  private SseEmitterManager sseEmitterManager;

  @BeforeEach
  void setUp() {
    sseEmitterManager = new SseEmitterManager(missedNotificationSender);
  }

  @Test
  @DisplayName("SSE 연결 시 SseEmitter가 반환된다.")
  void connect_success() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    SseEmitter emitter = sseEmitterManager.connect(userId, null);

    // then
    assertThat(emitter).isNotNull();
  }

  @Test
  @DisplayName("SSE 연결 후 동일 사용자가 재연결하면 새 Emitter로 교체된다.")
  void connect_replace_existing() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter first = sseEmitterManager.connect(userId, null);

    // when
    SseEmitter second = sseEmitterManager.connect(userId, null);

    // then
    assertThat(second).isNotSameAs(first);
  }

  @Test
  @DisplayName("연결된 사용자에게 이벤트 전송 시 예외가 발생하지 않는다.")
  void send_to_connected_user() {
    // given
    UUID userId = UUID.randomUUID();
    sseEmitterManager.connect(userId, null);

    // when & then
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, "test"));
  }

  @Test
  @DisplayName("연결되지 않은 사용자에게 전송해도 예외가 발생하지 않는다.")
  void send_to_unconnected_user() {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    assertDoesNotThrow(
        () -> sseEmitterManager.send(userId, "test")
    );
  }

  @Test
  @DisplayName("재연결 후 구 emitter의 onCompletion 콜백이 새 emitter를 제거하지 않는다.")
  void old_emitter_callback_does_not_remove_new_emitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter first = sseEmitterManager.connect(userId, null);
    sseEmitterManager.connect(userId, null); // 재연결 트리거 (반환값 미사용)

    // when - 구 emitter 타임아웃 강제 트리거
    first.completeWithError(new RuntimeException("timeout simulation"));

    // then
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, "test"));
  }

  @Test
  @DisplayName("재연결 후 구 emitter의 onTimeout 콜백이 새 emitter를 제거하지 않는다.")
  void old_emitter_timeout_does_not_remove_new_emitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter first = sseEmitterManager.connect(userId, null);
    sseEmitterManager.connect(userId, null); // 재연결 트리거 (반환값 미사용)

    // when - 구 emitter 타임아웃 강제 트리거
    first.completeWithError(new RuntimeException("timeout simulation"));

    // then
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, "test"));
  }

  @Test
  @DisplayName("Heartbeat 전송 시 연결된 emitter에 ping 이벤트가 전송된다.")
  void sendHeartbeat_success() {
    // given
    UUID userId = UUID.randomUUID();
    sseEmitterManager.connect(userId, null);

    // when & then
    assertDoesNotThrow(() -> sseEmitterManager.sendHeartbeat());
  }

  @Test
  @DisplayName("Heartbeat 전송 실패 시 해당 emitter가 맵에서 제거된다.")
  void sendHeartbeat_removes_completed_emitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = sseEmitterManager.connect(userId, null);
    emitter.complete();

    // when
    sseEmitterManager.sendHeartbeat();

    // then - 제거된 emitter에 send해도 예외 없음
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, "test"));
  }

  @Test
  @DisplayName("NotificationDto 전송 시 이벤트 id가 createdAt:id 형식으로 설정된다.")
  void send_notification_dto_sets_event_id() {
    // given
    UUID userId = UUID.randomUUID();
    sseEmitterManager.connect(userId, null);

    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(), Instant.now(), userId, "제목", "내용", NotificationLevel.INFO);

    // when & then
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, dto));
  }

  @Test
  @DisplayName("lastEventId가 있으면 유실 알림 재전송을 시도한다.")
  void connect_with_last_event_id_sends_missed_notifications() {
    // given
    UUID userId = UUID.randomUUID();
    String lastEventId = Instant.now() + ":" + UUID.randomUUID();

    // when
    sseEmitterManager.connect(userId, lastEventId);

    // then
    verify(missedNotificationSender).send(eq(userId), eq(lastEventId), any());
  }

  @Test
  @DisplayName("이벤트 전송 실패 시 emitter가 맵에서 제거된다.")
  void send_removes_emitter_on_failure() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter mockEmitter = mock(SseEmitter.class);
    doThrow(new IOException("전송 실패")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

    // emitters 맵에 직접 주입
    ReflectionTestUtils.setField(sseEmitterManager, "emitters",
        new java.util.concurrent.ConcurrentHashMap<>(Map.of(userId, mockEmitter)));

    // when
    sseEmitterManager.send(userId, "test");

    // then
    verify(mockEmitter).completeWithError(any());
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, "test"));
  }
}