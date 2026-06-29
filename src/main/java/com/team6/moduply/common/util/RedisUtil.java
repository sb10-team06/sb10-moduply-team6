package com.team6.moduply.common.util;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * Redis 통신을 위한 공통 유틸리티 클래스입니다.
 *
 * [도입 목적]
 * 1. 캡슐화: 각 Service 클래스에서 RedisTemplate의 내부 구현(opsForValue 등)을 몰라도 쉽게 캐시를 사용할 수 있습니다.
 * 2. 유연성: 만료 시간(TTL)이나 Key 조합 로직을 하드코딩하지 않고, 파라미터로 받아 여러 도메인에서 유연하게 재사용할 수 있습니다.
 * 3. 유지보수: 추후 공통 로직이 바뀌더라도 이 클래스 한 곳만 수정하면 프로젝트 전체에 반영됩니다.
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUtil {
  private final StringRedisTemplate stringRedisTemplate;

  @Retryable(
      retryFor = DataAccessException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 300, multiplier = 2)
  )
  public void setDateExpire(String key, String value, Duration duration) {
    ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
    valueOperations.set(key, value, duration);
  }

  @Retryable(
      retryFor = DataAccessException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 300, multiplier = 2)
  )
  public String getData(String key){
    ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
    return valueOperations.get(key);
  }

  @Recover
  public String recoverGetData(DataAccessException e, String key) {
    log.warn("Redis 조회 재시도 실패. key={}", key, e);
    return null;
  }

  @Retryable(
      retryFor = DataAccessException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 300, multiplier = 2)
  )
  public void deleteData(String key){
    stringRedisTemplate.delete(key);
  }

}
