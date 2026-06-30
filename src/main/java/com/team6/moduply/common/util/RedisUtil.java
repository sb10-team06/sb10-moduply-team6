package com.team6.moduply.common.util;

import java.time.Duration;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
  private static final DefaultRedisScript<String> ROTATE_REFRESH_TOKEN_SCRIPT = createRotateScript();

  /*
   * refresh token 회전은 자바에서 GET -> 비교 -> SET으로 나누면
   * 동시에 들어온 요청이 모두 "기존 토큰이 유효하다"고 판단할 수 있습니다.
   * 이 Lua 스크립트는 Redis 서버 내부에서 조회/비교/갱신을 한 번에 처리해서
   * 같은 refresh token의 중복 회전과 레이스 컨디션을 막습니다.
   *
   * 반환값:
   * - MISSING  : 현재 whitelist key가 없음
   * - MISMATCH : 저장된 토큰과 요청 토큰이 다름. key 삭제 후 거부
   * - OK       : 요청 토큰이 일치해 새 refresh token으로 원자적 갱신 성공
   */
  private static DefaultRedisScript<String> createRotateScript() {
    DefaultRedisScript<String> script = new DefaultRedisScript<>();
    script.setResultType(String.class);
    script.setScriptText("""
            local current = redis.call("GET", KEYS[1])
            
            if not current then
              return "MISSING"
            end
            
            if current ~= ARGV[1] then
              redis.call("DEL", KEYS[1])
              return "MISMATCH"
            end
            
            redis.call("PSETEX", KEYS[1], ARGV[3], ARGV[2])
            return "OK"  
            """);
    return script;
  }

  @Retryable(
      retryFor = DataAccessException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 300, multiplier = 2)
  )
  public String rotateRefreshToken(
      String key,
      String presentedRefreshToken,
      String newRefreshToken,
      Duration ttl
  ) {
    return stringRedisTemplate.execute(
        ROTATE_REFRESH_TOKEN_SCRIPT,
        Collections.singletonList(key),
        presentedRefreshToken,
        newRefreshToken,
        String.valueOf(ttl.toMillis())
    );
  }

  @Recover
  public String recoverRotateRefreshToken(
      DataAccessException e,
      String key,
      String presentedRefreshToken,
      String newRefreshToken,
      Duration ttl
  ) {
    log.error("Redis refresh token 원자 갱신 재시도 실패. key={}", key, e);
    throw e;
  }

  @Retryable(
      retryFor = DataAccessException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 300, multiplier = 2)
  )
  public void setDataExpire(String key, String value, Duration duration) {
    ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
    valueOperations.set(key, value, duration);
  }

  @Recover
  public void recoverSetDataExpire(
      DataAccessException e,
      String key,
      String value,
      Duration duration
  ) {
    log.error("Redis 저장 재시도 실패. key={}", key, e);
    throw e;
  }

  @Retryable(
      retryFor = DataAccessException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 300, multiplier = 2)
  )
  public String getData(String key) {
    ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
    return valueOperations.get(key);
  }

  @Recover
  public String recoverGetData(DataAccessException e, String key) {
    log.warn("Redis 조회 재시도 실패. key={}", key, e);
    throw e;
  }

  @Retryable(
      retryFor = DataAccessException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 300, multiplier = 2)
  )
  public void deleteData(String key) {
    stringRedisTemplate.delete(key);
  }

  @Recover
  public void recoverDeleteData(DataAccessException e, String key) {
    log.error("Redis 삭제 재시도 실패. key={}", key, e);
    throw e;
  }

}
