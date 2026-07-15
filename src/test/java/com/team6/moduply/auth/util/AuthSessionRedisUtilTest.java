package com.team6.moduply.auth.util;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthSessionRedisUtilTest {

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @InjectMocks
  private AuthSessionRedisUtil authSessionRedisUtil;

  @Test
  @DisplayName("세션 생성은 Lua 스크립트로 같은 브라우저의 기존 세션 폐기와 신규 세션 저장을 원자 처리한다")
  void create_session_executes_lua_script() {
    // Given
    UUID userId = UUID.randomUUID();
    String browserId = UUID.randomUUID().toString();
    String sessionId = UUID.randomUUID().toString();
    String browserSessionKey = RedisKeyPolicy.USER_BROWSER_SESSION.generateKey(
        userId + ":" + browserId
    );
    String sessionKey = RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId);

    // When
    authSessionRedisUtil.createSession(userId, browserId, sessionId);

    // Then
    verify(stringRedisTemplate).execute(
        eq(getCreateSessionScript()),
        eq(List.of(browserSessionKey, sessionKey)),
        eq(sessionId),
        eq(RedisKeyPolicy.AUTH_SESSION.getPrefix()),
        eq("ACTIVE"),
        eq("REVOKED"),
        eq(String.valueOf(RedisKeyPolicy.AUTH_SESSION.getTtl().toMillis())),
        eq(String.valueOf(RedisKeyPolicy.USER_BROWSER_SESSION.getTtl().toMillis()))
    );
  }

  @SuppressWarnings("unchecked")
  private DefaultRedisScript<String> getCreateSessionScript() {
    return (DefaultRedisScript<String>) ReflectionTestUtils
        .getField(AuthSessionRedisUtil.class, "CREATE_SESSION_SCRIPT");
  }
}
