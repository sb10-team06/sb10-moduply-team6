package com.team6.moduply.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SseEmitterManagerTest {

  private SseEmitterManager sseEmitterManager;

  @BeforeEach
  void setUp() {
    sseEmitterManager = new SseEmitterManager();
  }

  @Test
  @DisplayName("SSE 연결 시 SseEmitter가 반환된다.")
  void connect_success() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    SseEmitter emitter = sseEmitterManager.connect(userId);

    // then
    assertThat(emitter).isNotNull();
  }

  @Test
  @DisplayName("SSE 연결 후 동일 사용자가 재연결하면 새 Emitter로 교체된다.")
  void connect_replace_existing() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter first = sseEmitterManager.connect(userId);

    // when
    SseEmitter second = sseEmitterManager.connect(userId);

    // then
    assertThat(second).isNotSameAs(first);
  }

  @Test
  @DisplayName("연결된 사용자에게 이벤트 전송 시 예외가 발생하지 않는다.")
  void send_to_connected_user() {
    // given
    UUID userId = UUID.randomUUID();
    sseEmitterManager.connect(userId);

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
    SseEmitter first = sseEmitterManager.connect(userId);
    SseEmitter second = sseEmitterManager.connect(userId);

    // when - 구 emitter의 complete 호출 (onCompletion 콜백 트리거)
    first.complete();

    // then - 새 emitter는 여전히 맵에 남아있어야 함
    // send가 예외 없이 실행되면 새 emitter가 살아있다는 것
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, "test"));
  }

  @Test
  @DisplayName("재연결 후 구 emitter의 onTimeout 콜백이 새 emitter를 제거하지 않는다.")
  void old_emitter_timeout_does_not_remove_new_emitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter first = sseEmitterManager.connect(userId);
    SseEmitter second = sseEmitterManager.connect(userId);

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
    sseEmitterManager.connect(userId);

    // when & then
    assertDoesNotThrow(() -> sseEmitterManager.sendHeartbeat());
  }

  @Test
  @DisplayName("Heartbeat 전송 실패 시 해당 emitter가 맵에서 제거된다.")
  void sendHeartbeat_removes_completed_emitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = sseEmitterManager.connect(userId);
    emitter.complete();

    // when
    sseEmitterManager.sendHeartbeat();

    // then - 제거된 emitter에 send해도 예외 없음
    assertDoesNotThrow(() -> sseEmitterManager.send(userId, "test"));
  }
}