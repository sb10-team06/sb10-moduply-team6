package com.team6.moduply.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.util.AuthSessionRedisUtil;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

  @Mock
  private RedisUtil redisUtil;

  @Mock
  private AuthSessionRedisUtil authSessionRedisUtil;

  @Test
  @DisplayName("세션 생성은 sessionId를 생성한 뒤 Redis 세션 유틸에 위임한다")
  void create_session_delegates_to_auth_session_redis_util() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil, authSessionRedisUtil);
    UUID userId = UUID.randomUUID();
    String browserId = UUID.randomUUID().toString();

    // When
    String sessionId = authSessionService.createSession(userId, browserId);

    // Then
    assertThat(sessionId).isNotBlank();
    verify(authSessionRedisUtil).createSession(userId, browserId, sessionId);
  }

  @Test
  @DisplayName("세션 상태가 ACTIVE일 때만 활성 세션으로 판단한다")
  void is_session_active_returns_true_only_when_status_is_active() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil, authSessionRedisUtil);
    String activeSessionId = UUID.randomUUID().toString();
    String revokedSessionId = UUID.randomUUID().toString();

    given(redisUtil.getData(RedisKeyPolicy.AUTH_SESSION.generateKey(activeSessionId))).willReturn("ACTIVE");
    given(redisUtil.getData(RedisKeyPolicy.AUTH_SESSION.generateKey(revokedSessionId))).willReturn("REVOKED");

    // When & Then
    assertThat(authSessionService.isSessionActive(activeSessionId)).isTrue();
    assertThat(authSessionService.isSessionActive(revokedSessionId)).isFalse();
    assertThat(authSessionService.isSessionActive(null)).isFalse();
    assertThat(authSessionService.isSessionActive(" ")).isFalse();
  }

  @Test
  @DisplayName("세션 폐기 시 세션 상태를 REVOKED로 저장한다")
  void revoke_session_marks_session_as_revoked() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil, authSessionRedisUtil);
    String sessionId = UUID.randomUUID().toString();

    // When
    authSessionService.revokeSession(sessionId);

    // Then
    verify(redisUtil).setDataExpire(
        RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId),
        "REVOKED",
        RedisKeyPolicy.AUTH_SESSION.getTtl()
    );
  }

  @Test
  @DisplayName("빈 세션 ID는 revoke 하지 않는다")
  void revoke_session_ignores_blank_session_id() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil, authSessionRedisUtil);

    // When
    authSessionService.revokeSession(null);
    authSessionService.revokeSession(" ");

    // Then
    verify(redisUtil, never()).setDataExpire(any(), any(), eq(RedisKeyPolicy.AUTH_SESSION.getTtl()));
  }

}
