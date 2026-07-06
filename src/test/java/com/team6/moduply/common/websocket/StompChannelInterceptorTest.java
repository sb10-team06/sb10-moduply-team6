package com.team6.moduply.common.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
import com.team6.moduply.conversation.repository.ConversationRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class StompChannelInterceptorTest {

  @InjectMocks
  private StompChannelInterceptor stompChannelInterceptor;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private AuthService authService;

  @Mock
  private ConversationRepository conversationRepository;

  @Test
  @DisplayName("DM 구독 요청자가 대화방 참여자이면 구독을 허용한다.")
  void preSend_success_when_direct_message_subscriber_is_participant() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    String destination = "/sub/conversations/" + conversationId + "/direct-messages";
    Message<?> message = subscribeMessage(destination, userId);

    given(conversationRepository.existsByIdAndParticipantId(conversationId, userId))
        .willReturn(true);

    assertThatCode(() -> stompChannelInterceptor.preSend(message, null))
        .doesNotThrowAnyException();

    verify(conversationRepository).existsByIdAndParticipantId(conversationId, userId);
    verify(applicationEventPublisher).publishEvent(
        ArgumentMatchers.any(StompSubscribeEvent.class)
    );
  }

  @Test
  @DisplayName("DM 구독 요청자가 대화방 참여자가 아니면 구독을 거부한다.")
  void preSend_fail_when_direct_message_subscriber_is_not_participant() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    String destination = "/sub/conversations/" + conversationId + "/direct-messages";
    Message<?> message = subscribeMessage(destination, userId);

    given(conversationRepository.existsByIdAndParticipantId(conversationId, userId))
        .willReturn(false);

    assertThatThrownBy(() -> stompChannelInterceptor.preSend(message, null))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("대화방 참여자만 DM을 구독할 수 있습니다.");

    verify(conversationRepository).existsByIdAndParticipantId(conversationId, userId);
    verify(applicationEventPublisher, never()).publishEvent(
        ArgumentMatchers.any(StompSubscribeEvent.class)
    );
  }

  @Test
  @DisplayName("DM이 아닌 구독 요청은 대화방 참여자 검증을 수행하지 않는다.")
  void preSend_success_without_participant_validation_when_not_direct_message_subscription() {
    UUID userId = UUID.randomUUID();
    Message<?> message = subscribeMessage("/sub/watching-sessions/session-1", userId);

    assertThatCode(() -> stompChannelInterceptor.preSend(message, null))
        .doesNotThrowAnyException();

    verify(conversationRepository, never()).existsByIdAndParticipantId(
        ArgumentMatchers.any(UUID.class),
        ArgumentMatchers.any(UUID.class)
    );
    verify(applicationEventPublisher).publishEvent(
        ArgumentMatchers.any(StompSubscribeEvent.class)
    );
  }

  private Message<?> subscribeMessage(String destination, UUID userId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    accessor.setSubscriptionId("sub-1");
    accessor.setSessionId("session-1");
    accessor.setSessionAttributes(new ConcurrentHashMap<>(Map.of("userId", userId)));
    accessor.setUser(new TestingAuthenticationToken("user", null));

    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
