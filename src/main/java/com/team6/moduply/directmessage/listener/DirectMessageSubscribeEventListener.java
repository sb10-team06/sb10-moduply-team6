package com.team6.moduply.directmessage.listener;

import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
import com.team6.moduply.conversation.repository.ConversationRepository;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageSubscribeEventListener {

  private static final Pattern DIRECT_MESSAGE_SUBSCRIBE_DESTINATION = Pattern.compile(
      "^/sub/conversations/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/direct-messages$"
  );
  private static final Pattern DIRECT_MESSAGE_SUBSCRIBE_PATH = Pattern.compile(
      "^/sub/conversations/.+/direct-messages$"
  );

  private final ConversationRepository conversationRepository;

  @EventListener
  public void on(StompSubscribeEvent event) {
    String destination = event.destination();
    Matcher matcher = DIRECT_MESSAGE_SUBSCRIBE_DESTINATION.matcher(destination);

    if (!matcher.matches()) {
      if (DIRECT_MESSAGE_SUBSCRIBE_PATH.matcher(destination).matches()) {
        throw new MessageDeliveryException("올바르지 않은 대화방 ID 형식입니다.");
      }
      return;
    }

    UUID conversationId = UUID.fromString(matcher.group(1));
    if (!conversationRepository.existsByIdAndParticipantId(conversationId, event.userId())) {
      log.warn("[STOMP] DM 구독 권한 없음. sessionId={}, userId={}, conversationId={}",
          event.sessionId(), event.userId(), conversationId);
      throw new MessageDeliveryException("대화방 참여자만 DM을 구독할 수 있습니다.");
    }
  }
}
