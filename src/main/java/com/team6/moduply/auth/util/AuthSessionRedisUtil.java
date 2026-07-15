package com.team6.moduply.auth.util;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthSessionRedisUtil {

  private static final String ACTIVE = "ACTIVE";
  private static final String REVOKED = "REVOKED";

  /*
   * 같은 사용자 + 같은 브라우저의 세션 교체는 GET -> SET 흐름으로 나누면
   * 동시 로그인 요청에서 이전 세션이 ACTIVE로 남을 수 있습니다.
   * 이 Lua 스크립트는 Redis 서버 내부에서 다음 작업을 원자적으로 처리합니다.
   *
   * 1. 현재 브라우저가 가리키는 기존 sessionId 조회
   * 2. 기존 sessionId가 있으면 auth:session:{previousSessionId}를 REVOKED로 저장
   * 3. 신규 auth:session:{newSessionId}를 ACTIVE로 저장
   * 4. auth:user-browser-session:{userId}:{browserId}를 신규 sessionId로 갱신
   */
  private static final DefaultRedisScript<String> CREATE_SESSION_SCRIPT = createSessionScript();

  private final StringRedisTemplate stringRedisTemplate;

  private static DefaultRedisScript<String> createSessionScript() {
    DefaultRedisScript<String> script = new DefaultRedisScript<>();
    script.setResultType(String.class);
    script.setScriptText("""
            local previousSessionId = redis.call("GET", KEYS[1])

            if previousSessionId and previousSessionId ~= "" then
              redis.call("PSETEX", ARGV[2] .. previousSessionId, ARGV[5], ARGV[4])
            end

            redis.call("PSETEX", KEYS[2], ARGV[5], ARGV[3])
            redis.call("PSETEX", KEYS[1], ARGV[6], ARGV[1])

            return previousSessionId or ""
            """);
    return script;
  }

  public void createSession(UUID userId, String browserId, String sessionId) {
    String browserSessionKey = RedisKeyPolicy.USER_BROWSER_SESSION.generateKey(
        userId + ":" + browserId
    );
    String sessionKey = RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId);

    stringRedisTemplate.execute(
        CREATE_SESSION_SCRIPT,
        List.of(browserSessionKey, sessionKey),
        sessionId,
        RedisKeyPolicy.AUTH_SESSION.getPrefix(),
        ACTIVE,
        REVOKED,
        ttlMillis(RedisKeyPolicy.AUTH_SESSION.getTtl()),
        ttlMillis(RedisKeyPolicy.USER_BROWSER_SESSION.getTtl())
    );
  }

  private String ttlMillis(Duration ttl) {
    return String.valueOf(ttl.toMillis());
  }
}
