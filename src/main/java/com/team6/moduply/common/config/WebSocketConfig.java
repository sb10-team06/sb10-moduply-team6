package com.team6.moduply.common.config;

import com.team6.moduply.common.websocket.CorsProperties;
import com.team6.moduply.common.websocket.StompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // 하트비트 이벤트 관리 단일 스레드 스케줄러
    ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
    heartbeatScheduler.setPoolSize(1);
    heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-thread-");
    heartbeatScheduler.initialize();

    // 10초 주기로 핑 전송, 20초 이내 핑 접수 없으면 연결 끊기
    config.enableSimpleBroker("/sub")
        .setHeartbeatValue(new long[]{10000, 20000})
        .setTaskScheduler(heartbeatScheduler);

    config.setApplicationDestinationPrefixes("/pub");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
        .withSockJS();
  }

  // TODO: [김민형] 인가 추가(jwt 로직 개발 이후)
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        stompChannelInterceptor
    );
  }
}
