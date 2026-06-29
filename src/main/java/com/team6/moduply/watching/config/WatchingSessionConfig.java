package com.team6.moduply.watching.config;

import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import com.team6.moduply.watching.repository.impl.InMemoryWatchingSessionRepository;
import com.team6.moduply.watching.repository.impl.RedisWatchingSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 시청 세션 저장소(WatchingSessionRepository)의 전략을 설정하는 구성 클래스입니다.
 *
 * <p><b>주요 특징:</b></p>
 * <ul>
 * <li>환경 설정({@code watching.storage.type})에 따라 실행 시점에 저장소 구현체를 동적으로 주입합니다.</li>
 * <li>로컬 개발 환경이나 테스트 환경에서는 인메모리 방식을, 분산 서버 환경에서는 Redis 방식을 선택적으로 운용할 수 있습니다.</li>
 * </ul>
 *
 * @author 김민형
 * @since 26. 6. 24.
 */
@Configuration
@Slf4j
public class WatchingSessionConfig {

  /**
   * 인메모리 기반의 시청 세션 저장소 빈을 등록합니다.
   * <p>프로퍼티 설정이 {@code in-memory}이거나, 관련 설정이 명시되지 않은 경우(기본값) 활성화됩니다.</p>
   */
  @Bean
  @ConditionalOnProperty(name = "watching.storage.type", havingValue = "in-memory", matchIfMissing = true)
  public WatchingSessionRepository inmemoryWatchingSessionRepository() {
    log.info("▶︎ In-memory WatchingSessionRepository initialized");
    return new InMemoryWatchingSessionRepository();
  }

  /**
   * Redis 기반의 분산 시청 세션 저장소 빈을 등록합니다.
   * <p>프로퍼티 설정이 {@code redis}로 명시된 인스턴스 환경에서 활성화됩니다.</p>
   */
  @Bean
  @ConditionalOnProperty(name = "watching.storage.type", havingValue = "redis")
  public WatchingSessionRepository redisWatchingSessionRepository(
      RedisTemplate<String, WatchingSession> redisTemplate,
      RedisTemplate<String, String> sessionIdAndUserIdRedisTemplate
  ) {
    log.info("▶︎ Redis WatchingSessionRepository initialized");
    return new RedisWatchingSessionRepository(redisTemplate, sessionIdAndUserIdRedisTemplate);
  }
}
