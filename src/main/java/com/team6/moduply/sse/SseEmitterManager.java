package com.team6.moduply.sse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Slf4j
public class SseEmitterManager {

  private static final long TIMEOUT = 60 * 60 * 1000L; // 1시간
  private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter connect(UUID userId) {
    SseEmitter emitter = new SseEmitter(TIMEOUT);

    emitter.onCompletion(() -> {
      log.info("[SSE] 연결 완료 - userId: {}", userId);
      emitters.remove(userId, emitter);
    });

    emitter.onTimeout(() -> {
      log.info("[SSE] 연결 타임아웃 - userId: {}", userId);
      emitters.remove(userId, emitter);
    });

    emitter.onError(e -> {
      log.error("[SSE] 연결 오류 - userId: {}", userId, e);
      emitters.remove(userId, emitter);
    });

    emitters.put(userId, emitter);

    // 연결 직후 더미 이벤트 전송 (503 방지)
    try {
      emitter.send(SseEmitter.event()
          .name("connect")
          .data("connected"));
    } catch (IOException e) {
      log.error("[SSE] 초기 이벤트 전송 실패 - userId: {}", userId, e);
      emitters.remove(userId);
    }

    return emitter;
  }

  public void send(UUID userId, Object data) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter == null) {
      return;
    }
    try {
      emitter.send(SseEmitter.event()
          .name("notification")
          .data(data));
    } catch (IOException e) {
      log.error("[SSE] 초기 이벤트 전송 실패 - userId: {}", userId, e);
      emitters.remove(userId, emitter);
    }
  }
}