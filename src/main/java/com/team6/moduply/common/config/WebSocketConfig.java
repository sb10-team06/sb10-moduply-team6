package com.team6.moduply.common.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.CollectionUtils;
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
@ConfigurationProperties(prefix = "cors")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Getter
  @Setter
  private List<String> allowedOrigins;

  @PostConstruct
  public void validate() {
    if (CollectionUtils.isEmpty(allowedOrigins)) {
      throw new IllegalStateException("웹소켓 설정 오류: 'cors.allowed-origins' 가 비어있거나 누락되었습니다");
    }
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/sub");
    config.setApplicationDestinationPrefixes("/pub");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns(allowedOrigins.toArray(new String[0]))
        .withSockJS();
  }

  // TODO: [김민형] configureClientInboundChannel 추가(jwt 로직 개발 이후)

}
