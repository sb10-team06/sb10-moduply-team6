package com.team6.moduply.watching.listener;

import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
import com.team6.moduply.common.websocket.events.StompUnSubscribeEvent;
import com.team6.moduply.watching.command.CreateWatchingSessionCommand;
import com.team6.moduply.watching.dto.WatchingSessionDto;
import com.team6.moduply.watching.service.WatchingSessionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionEventListener {

  private final WatchingSessionService watchingSessionService;

  @EventListener
  public void on(StompSubscribeEvent event) {
    log.debug("[Event Trace] StompSubscribeEvent 수신 완료: dest={}, userId={}", event.destination(),
        event.userId());
    String destination = event.destination();
    try {
      if (destination.startsWith("/sub/contents/") && destination.endsWith("/watch")) {
        UUID contentId = parseContentId(destination);
        log.debug("[EVENT TRACE] WatchingSessionService 저장 호출: userId={}, contentId: {}",
            event.userId(),
            contentId);
        watchingSessionService.create(new CreateWatchingSessionCommand(
            event.sessionId(),
            event.userId(),
            contentId
        ));
      } else if (destination.startsWith("/sub/contents/") && destination.endsWith("/chat")) {
        log.debug("[EVENT TRACE] WatchingSessionService 조회 호출: userId={}", event.userId());
        WatchingSessionDto dto = watchingSessionService.findByUserId(event.userId());
        UUID contentId = parseContentId(destination);
        if (dto == null || !contentId.equals(dto.content().id())) {
          log.error("시청 세션이 없는 사용자의 채팅창 구독 요청 또는, 시청세션과 다른 콘텐츠 경로로의 구독 요청");
          throw new MessageDeliveryException("잘못된 경로의 컨텐츠 실시간 구독 요청입니다.");
        }
      }
      log.info("[EVENT TRACE] StompSubscribeEvent 처리 완료: dest={}, userId={}", event.destination(),
          event.userId());
    } catch (IllegalArgumentException e) {
      log.warn("[STOMP] 잘못된 형식의 구독 요청 - 세션 ID: {}, 경로: {}", event.sessionId(), event.destination(),
          e);
      throw new MessageDeliveryException("올바르지 않은 콘텐츠 ID 형식입니다.");
    }
  }

  @EventListener
  public void on(StompUnSubscribeEvent event) {
    log.debug("[EVENT TRACE] StompUnsubscribeEvent 수신 완료: dest={}, sessionId={}",
        event.destination(), event.sessionId());
    String destination = event.destination();
    if (destination.startsWith("/sub/contents/") && destination.endsWith("/watch")) {
      log.debug("[EVENT TRACE] WatchingSessionService 호출: sessionId={}", event.sessionId());
      watchingSessionService.delete(event.sessionId());
      log.info("[EVENT TRACE] StompUnsubscribeEvent 처리 완료: sessionId={}", event.sessionId());
    }
  }

  @EventListener
  public void on(SessionDisconnectEvent event) {
    log.debug("[EVENT TRACE] SessionDisconnectEvent 수신 완료: sessionId={}", event.getSessionId());
    watchingSessionService.delete(event.getSessionId());
    log.info("[EVENT TRACE] SessionDisconnectEvent 처리 완료: sessionId={}", event.getSessionId());
  }

  private UUID parseContentId(String destination) {
    String[] split = destination.split("/");
    return UUID.fromString(split[3]);
  }
}
