package com.team6.moduply.sse;

import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.service.NotificationService;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseMissedNotificationSender {

  private final NotificationService notificationService;

  public void send(UUID userId, String lastEventId, SseEmitter emitter) {
    if (lastEventId == null) return;

    String[] parts = lastEventId.split(":");
    if (parts.length < 2) return;

    Instant lastCreatedAt = Instant.parse(parts[0]);
    UUID lastId = UUID.fromString(parts[parts.length - 1]);

    for (NotificationDto dto : notificationService.findMissedNotifications(userId, lastCreatedAt, lastId)) {
      try {
        emitter.send(SseEmitter.event()
            .id(dto.createdAt() + ":" + dto.id())
            .name("notifications")
            .data(dto));
        log.debug("[SSE] 유실 알림 재전송 - userId={}, notificationId={}", userId, dto.id());
      } catch (IOException | IllegalStateException e) {
        log.error("[SSE] 유실 알림 재전송 실패 - userId={}", userId, e);
        emitter.completeWithError(e);
        break;
      }
    }
  }
}
