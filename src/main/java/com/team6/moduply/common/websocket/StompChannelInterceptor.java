package com.team6.moduply.common.websocket;

import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
import com.team6.moduply.common.websocket.events.StompUnSubscribeEvent;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

  private final ApplicationEventPublisher applicationEventPublisher;
  private static final String SUBSCRIPTIONS_KEY = "SUBSCRIPTIONS";

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
      // TODO: [김민형] jwt 토큰 인증은 향후 작업 단위에서 추가할 예정입니다.
      case CONNECT:
        String token = resolveToken(accessor)
            .orElseThrow(() -> new RuntimeException("토큰 없음"));
//        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
//            token, null);
//        accessor.setUser(authenticationToken);
//        if (sessionAttributes != null) {
//          sessionAttributes.put("userId", UUID.randomUUID());
//        }
//        break;
        UUID parsedUserId;
        try {
          // 테스트환경
          parsedUserId = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
          // 💡프론트 환경!
          parsedUserId = UUID.randomUUID();
        }
        
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            parsedUserId, null, null);
        accessor.setUser(authenticationToken);

        if (sessionAttributes != null) {
          sessionAttributes.put("userId", parsedUserId);
        }
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
    String destination = accessor.getDestination();

    if (subscriptionId != null && destination != null) {
      subscriptionMap.put(subscriptionId, destination);
      UUID userId = (UUID) sessionAttributes.get("userId");
      if (userId == null) {
        throw new MessageDeliveryException("웹소켓 세션에 사용자 인증 정보가 존재하지 않습니다.");
      }
      applicationEventPublisher.publishEvent(new StompSubscribeEvent(
          accessor.getSessionId(),
          userId,
          destination
      ));
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
}