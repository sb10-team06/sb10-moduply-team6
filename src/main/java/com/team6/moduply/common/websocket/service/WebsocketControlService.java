package com.team6.moduply.common.websocket.service;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebsocketControlService {

  private final MessageChannel clientOutboundChannel;
  private final SimpUserRegistry userRegistry;
  private static final String FORCE_DISCONNECT_MESSAGE = "FORCE_CLOSE_BY_SERVER";

  public void sendForceLogoutSignal(String email) {

    SimpUser simpUser = userRegistry.getUser(email);

    if (simpUser == null) {
      return;
    }
    String message = "Disconnect By Logout";
    simpUser.getSessions().forEach(session -> sendErrorFrame(session.getId(), message));
  }

  private void sendErrorFrame(String sessionId, String payloadMessage) {
    try {
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);

      accessor.setSessionId(sessionId);
      accessor.setMessage(FORCE_DISCONNECT_MESSAGE); // 에러 사유 메시지

      byte[] payload = payloadMessage == null ? new byte[0] : payloadMessage.getBytes(
          StandardCharsets.UTF_8);

      Message<byte[]> message = MessageBuilder.createMessage(
          payload,
          accessor.getMessageHeaders()
      );

      clientOutboundChannel.send(message);
    } catch (Exception e) {
      log.warn("Failed to send disconnect message. sessionId={}", sessionId, e);
    }
  }
}
