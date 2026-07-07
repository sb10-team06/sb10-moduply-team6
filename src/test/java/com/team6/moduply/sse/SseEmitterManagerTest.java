package com.team6.moduply.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
  @DisplayName("연결되지 않은 사용자에게 전송해도 예외가 발생하지 않는다.")
  void send_to_unconnected_user() {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> sseEmitterManager.send(userId, "test")
    );
  }
}