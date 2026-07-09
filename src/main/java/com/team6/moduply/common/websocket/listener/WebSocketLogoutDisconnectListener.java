package com.team6.moduply.common.websocket.listener;

import com.team6.moduply.auth.event.UserLogoutEvent;
import com.team6.moduply.common.websocket.service.WebsocketControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketLogoutDisconnectListener {

  private final WebsocketControlService websocketConnectionManager;

  @EventListener
  public void on(UserLogoutEvent event) {
    log.debug("[LOGOUT EVENT] Disconnecting user: {}", event.email());
    try {
      websocketConnectionManager.sendForceLogoutSignal(event.email());
    } catch (Exception e) {
      log.warn("[LOGOUT EVENT] Failed to send disconnect message.", e);
    }
  }

}
