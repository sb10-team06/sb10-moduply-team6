package com.team6.moduply.common.websocket;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

  @Getter
  @Setter
  private List<String> allowedOrigins;

  @PostConstruct
  public void validate() {
    if (CollectionUtils.isEmpty(allowedOrigins)) {
      throw new IllegalStateException("웹소켓 설정 오류: 'cors.allowed-origins' 가 비어있거나 누락되었습니다");
    }
  }
}
