package com.team6.moduply.auth.util;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import java.time.Duration;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenRedisUtil {

  private final StringRedisTemplate stringRedisTemplate;

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
  private static final DefaultRedisScript<String> ROTATE_REFRESH_TOKEN_SCRIPT = createScript();

  private static DefaultRedisScript<String> createScript() {
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

  public String rotate(String email, String presentedRefreshToken, String newRefreshToken) {
    String redisKey = RedisKeyPolicy.REFRESH_TOKEN.generateKey(email);
    Duration ttl = RedisKeyPolicy.REFRESH_TOKEN.getTtl();

    return stringRedisTemplate.execute(
        ROTATE_REFRESH_TOKEN_SCRIPT,
        Collections.singletonList(redisKey),
        presentedRefreshToken,
        newRefreshToken,
        String.valueOf(ttl.toMillis())
    );
  }
}
