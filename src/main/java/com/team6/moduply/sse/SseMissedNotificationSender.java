package com.team6.moduply.sse;

import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.service.NotificationService;
import java.io.IOException;
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

  public void send(UUID userId, UUID lastEventId, SseEmitter emitter) {
    if (lastEventId == null) return;

    for (NotificationDto dto : notificationService.findMissedNotifications(userId, lastEventId)) {
      try {
        emitter.send(SseEmitter.event()
            .id(dto.id().toString())
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
