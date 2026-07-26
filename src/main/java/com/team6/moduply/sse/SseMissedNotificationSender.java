package com.team6.moduply.sse;

import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.service.NotificationService;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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

    int lastColonIndex = lastEventId.lastIndexOf(":");
    if (lastColonIndex < 0) {
      log.warn("[SSE] 잘못된 Last-Event-ID 형식 - userId={}, lastEventId={}", userId, lastEventId);
      return;
    }

    Instant lastCreatedAt;
    UUID lastId;
    try {
      lastCreatedAt = Instant.parse(lastEventId.substring(0, lastColonIndex));
      lastId = UUID.fromString(lastEventId.substring(lastColonIndex + 1));
    } catch (DateTimeParseException | IllegalArgumentException e) {
      log.warn("[SSE] Last-Event-ID 파싱 실패 - userId={}, lastEventId={}", userId, lastEventId, e);
      return;
    }

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
