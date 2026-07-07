package com.team6.moduply.common.config;

import com.team6.moduply.common.websocket.CorsProperties;
import com.team6.moduply.common.websocket.StompChannelInterceptor;
import com.team6.moduply.user.enums.Role;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 클래스명: WebSocketConfig 설명: 웹소켓 및 STOMP 설정을 담당합니다.
 *
 * <p> 주요 통신 규칙
 * 1. 엔드 포인트 "/ws" (SocketJS 지원) 2. 구독 경로 "/sub" 3. 발행 경로 "/pub" 4. 허용 오리진 환경변수 주입</p>
 *
 * @author 김민형
 * @since 26. 6. 23.
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompChannelInterceptor stompChannelInterceptor;
  private final CorsProperties corsProperties;

  // 하트비트 이벤트 관리 단일 스레드 스케줄러
  @Bean(destroyMethod = "destroy")
  public ThreadPoolTaskScheduler heartbeatScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("ws-heartbeat-thread-");
    scheduler.initialize();
    return scheduler;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {

    // 10초 주기로 핑 전송, 20초 이내 핑 접수 없으면 연결 끊기
    config.enableSimpleBroker("/sub")
        .setHeartbeatValue(new long[]{10000, 20000})
        .setTaskScheduler(heartbeatScheduler());

    config.setApplicationDestinationPrefixes("/pub");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
        .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        stompChannelInterceptor,
        new SecurityContextChannelInterceptor(),
        authorizationChannelInterceptor()
    );
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
  }

  private AuthorizationChannelInterceptor authorizationChannelInterceptor() {
    // TODO: WebSocket 인가에도 RoleHierarchy를 직접 연결할지, 인증 객체에 확장 권한을 넣을지
    //  리팩토링 단계에서 다시 논의한다.
    return new AuthorizationChannelInterceptor(
        MessageMatcherDelegatingAuthorizationManager.builder()
            .anyMessage().hasRole(Role.USER.name())
            .build()
    );
  }
}
