package com.team6.moduply.common.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
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

  @Test
  @DisplayName("SUBSCRIBE 요청을 받으면 구독 이벤트를 발행하고 세션에 구독 정보를 저장한다.")
  void preSend_success_when_subscribe() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    String destination = "/sub/conversations/" + conversationId + "/direct-messages";
    Map<String, Object> sessionAttributes = new ConcurrentHashMap<>(Map.of("userId", userId));
    Message<?> message = subscribeMessage(destination, sessionAttributes);

    assertThatCode(() -> stompChannelInterceptor.preSend(message, null))
        .doesNotThrowAnyException();

    verify(applicationEventPublisher).publishEvent(
        ArgumentMatchers.any(StompSubscribeEvent.class)
    );
    Map<String, String> subscriptions = (Map<String, String>) sessionAttributes.get("SUBSCRIPTIONS");
    assertThat(subscriptions).containsEntry("sub-1", destination);
  }

  @Test
  @DisplayName("구독 이벤트 처리 중 예외가 발생하면 구독 정보를 저장하지 않는다.")
  void preSend_fail_when_subscribe_event_listener_throws_exception() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    String destination = "/sub/conversations/" + conversationId + "/direct-messages";
    Map<String, Object> sessionAttributes = new ConcurrentHashMap<>(Map.of("userId", userId));
    Message<?> message = subscribeMessage(destination, sessionAttributes);

    willThrow(new MessageDeliveryException("대화방 참여자만 DM을 구독할 수 있습니다."))
        .given(applicationEventPublisher)
        .publishEvent(ArgumentMatchers.any(StompSubscribeEvent.class));
    assertThatThrownBy(() -> stompChannelInterceptor.preSend(message, null))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("대화방 참여자만 DM을 구독할 수 있습니다.");

    Map<String, String> subscriptions = (Map<String, String>) sessionAttributes.get("SUBSCRIPTIONS");
    assertThat(subscriptions).doesNotContainKey("sub-1");
  }

  @Test
  @DisplayName("인증 사용자 정보가 없으면 구독 이벤트를 발행하지 않는다.")
  void preSend_fail_without_user_id() {
    Map<String, Object> sessionAttributes = new ConcurrentHashMap<>();
    Message<?> message = subscribeMessage("/sub/watching-sessions/session-1", sessionAttributes);

    assertThatThrownBy(() -> stompChannelInterceptor.preSend(message, null))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("웹소켓 세션에 사용자 인증 정보가 존재하지 않습니다.");

    verify(applicationEventPublisher, never()).publishEvent(
        ArgumentMatchers.any(StompSubscribeEvent.class)
    );
  }

  private Message<?> subscribeMessage(String destination, Map<String, Object> sessionAttributes) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    accessor.setSubscriptionId("sub-1");
    accessor.setSessionId("session-1");
    accessor.setSessionAttributes(sessionAttributes);
    accessor.setUser(new TestingAuthenticationToken("user", null));

    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
