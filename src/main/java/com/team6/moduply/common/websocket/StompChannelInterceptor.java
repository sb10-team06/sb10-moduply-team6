package com.team6.moduply.common.websocket;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
import com.team6.moduply.common.websocket.events.StompUnSubscribeEvent;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

  //JWT 인증
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthService authService;

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

  /// STOMP SUBSCRIBE 요청이 들어왔을때 처리하는 메서드.
  private void handleSubscribe(StompHeaderAccessor accessor,
      Map<String, Object> sessionAttributes) {
    // 세션 정보가 없으면 return
    // sessionAttributes에는 로그인할 때 저장해둔 값 ex) userId가 들어있어야한다.
    if (sessionAttributes == null) {
      return;
    }
    // WebSocket 세션이 구독중인 목록을 저장할 Map을 꺼낸다.
    // 없으면 새로 만든다.
    // ex) SUBSCRIPTIONS = { "sub-1": "/sub/conversations/11111111-1111-1111-1111-111111111111/direct-messages" }
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

      applicationEventPublisher.publishEvent(new StompSubscribeEvent(
              accessor.getSessionId(),
              userId,
              destination
      ));

      subscriptionMap.put(subscriptionId, destination);
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
      if (authService.isAccessTokenBlacklisted(token)) {
        throw new BadCredentialsException("로그아웃된 엑세스 토큰입니다.");
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
