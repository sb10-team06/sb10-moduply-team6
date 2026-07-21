package com.team6.moduply.sse;

import com.team6.moduply.notification.dto.NotificationDto;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

  private static final long TIMEOUT = 60 * 60 * 1000L; // 1시간
  private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final SseMissedNotificationSender missedNotificationSender;

  public SseEmitter connect(UUID userId, UUID lastEventId) {
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
      emitter.completeWithError(e);;
      emitters.remove(userId, emitter);
    }

    // lastEventId 이후 유실된 알림 재전송
    missedNotificationSender.send(userId, lastEventId, emitter);

    return emitter;
  }

  public void send(UUID userId, Object data) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter == null) {
      return;
    }
    try {
      SseEmitter.SseEventBuilder event = SseEmitter.event()
          .name("notifications")
          .data(data);

      // NotificationDto인 경우 id 설정
      if (data instanceof NotificationDto dto) {
        event.id(dto.id().toString());
      }

      emitter.send(event);
    } catch (IOException | IllegalStateException e) {
      log.error("[SSE] 이벤트 전송 실패 - userId={}", userId, e);
      emitter.completeWithError(e);
      emitters.remove(userId, emitter);
    }
  }

  @Scheduled(fixedDelay = 30000) // 30초마다
  public void sendHeartbeat() {
    if (emitters.isEmpty()) {
      return;
    }
    log.debug("[SSE] Heartbeat 전송 - 연결 수: {}", emitters.size());
    emitters.forEach((userId, emitter) -> {
      try {
        emitter.send(SseEmitter.event()
            .name("heartbeat")
            .data("ping"));
      } catch (IOException | IllegalStateException e) {
        log.error("[SSE] Heartbeat 전송 실패 - userId={}", userId, e);
        emitter.completeWithError(e);
        emitters.remove(userId, emitter);
      }
    });
  }
}