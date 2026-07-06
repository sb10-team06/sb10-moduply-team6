package com.team6.moduply.common.websocket;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
import com.team6.moduply.common.websocket.events.StompUnSubscribeEvent;
import com.team6.moduply.conversation.repository.ConversationRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

  private final ApplicationEventPublisher applicationEventPublisher;
  private static final String SUBSCRIPTIONS_KEY = "SUBSCRIPTIONS";
  private static final Pattern DIRECT_MESSAGE_SUBSCRIBE_DESTINATION = Pattern.compile(
    "^/sub/conversations/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/direct-messages$"
  );

  //JWT 인증
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthService authService;
  private final ConversationRepository conversationRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
        StompHeaderAccessor.class);
    StompCommand command = accessor.getCommand();
    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

    if (command == null) {
      return message;
    }

    switch (command) {
      case CONNECT:
        String token = resolveToken(accessor)
            .orElseThrow(() -> new BadCredentialsException("유효하지 않는 토큰"));
        setAuthentication(accessor, sessionAttributes, token);
        break;

      case SUBSCRIBE:
        validateAuthentication(accessor);
        handleSubscribe(accessor, sessionAttributes);
        break;
      case UNSUBSCRIBE:
        validateAuthentication(accessor);
        handleUnSubscribe(accessor, sessionAttributes);
        break;
      case DISCONNECT:
        break;
      default:
        validateAuthentication(accessor);
    }

    return message;
  }

  private void validateAuthentication(StompHeaderAccessor accessor) {
    if (accessor.getUser() == null) {
      throw new MessageDeliveryException("인증되지 않은 웹소켓 사용자입니다.");
    }
  }

  private void handleSubscribe(StompHeaderAccessor accessor,
      Map<String, Object> sessionAttributes) {

    if (sessionAttributes == null) {
      return;
    }

    Map<String, String> subscriptionMap = (Map<String, String>) sessionAttributes.computeIfAbsent(
        SUBSCRIPTIONS_KEY,
        key -> new ConcurrentHashMap<>());

    String subscriptionId = accessor.getSubscriptionId();
    // ex) destination: /sub/conversations/{conversationId}/direct-messages
    String destination = accessor.getDestination();

    if (subscriptionId != null && destination != null) {
      UUID userId = (UUID) sessionAttributes.get("userId");

      if (userId == null) {
        throw new MessageDeliveryException("웹소켓 세션에 사용자 인증 정보가 존재하지 않습니다.");
      }

      validateDirectMessageSubscription(destination, userId);

      subscriptionMap.put(subscriptionId, destination);

      applicationEventPublisher.publishEvent(new StompSubscribeEvent(
              accessor.getSessionId(),
              userId,
              destination
      ));
    }
  }

  private void validateDirectMessageSubscription(String destination, UUID userId) {
    Matcher matcher = DIRECT_MESSAGE_SUBSCRIBE_DESTINATION.matcher(destination);
    if (!matcher.matches()) {
      return;
    }

    UUID conversationId = UUID.fromString(matcher.group(1));
    // 해당 conversationId에 userId가 참여자인지 검증.
    if (!conversationRepository.existsByIdAndParticipantId(conversationId, userId)) {
      throw new MessageDeliveryException("대화방 참여자만 DM을 구독할 수 있습니다.");
    }
  }

  private void handleUnSubscribe(StompHeaderAccessor accessor,
      Map<String, Object> sessionAttributes) {

    if (sessionAttributes == null) {
      return;
    }

    Map<String, String> subscriptionMap = (Map<String, String>) sessionAttributes.get(
        SUBSCRIPTIONS_KEY);

    if (subscriptionMap == null || accessor.getSubscriptionId() == null) {
      return;
    }

    String destination = subscriptionMap.remove(accessor.getSubscriptionId());
    if (destination == null) {
      return;
    }

    applicationEventPublisher.publishEvent(
        new StompUnSubscribeEvent(accessor.getSessionId(), destination));
    if (subscriptionMap.isEmpty()) {
      sessionAttributes.remove(SUBSCRIPTIONS_KEY);
    }
  }

  private Optional<String> resolveToken(StompHeaderAccessor accessor) {
    String prefix = "Bearer ";
    return Optional.ofNullable(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION))
        .map(value -> {
          if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
          } else {
            return null;
          }
        });
  }

  private void setAuthentication(StompHeaderAccessor accessor,
      Map<String, Object> sessionAttributes, String token) {
    try {
      if (!jwtTokenProvider.validateAccessToken(token)) {
        throw new BadCredentialsException("엑세스 토큰이 유효하지 않습니다.");
      }
      UUID userId = jwtTokenProvider.getUserId(token);
      Authentication authentication = authService.getAuthentication(userId);
      accessor.setUser(authentication);
      if (sessionAttributes != null) {
        sessionAttributes.put("userId", userId);
      }
    } catch (AuthenticationException e) {
      throw new BadCredentialsException("엑세스 토큰이 유효하지 않습니다.", e);
    } catch (Exception e) {
      throw new MessageDeliveryException("웹소켓 연결 인증에 실패하였습니다.");
    }
  }
}
