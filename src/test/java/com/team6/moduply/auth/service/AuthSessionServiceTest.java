package com.team6.moduply.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

  @Mock
  private RedisUtil redisUtil;

  @Test
  @DisplayName("같은 브라우저의 기존 세션이 있으면 REVOKED 처리하고 새 세션을 ACTIVE로 저장한다")
  void create_session_revokes_previous_browser_session_and_creates_new_session() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil);
    UUID userId = UUID.randomUUID();
    String browserId = UUID.randomUUID().toString();
    String previousSessionId = UUID.randomUUID().toString();
    String browserSessionKey = RedisKeyPolicy.USER_BROWSER_SESSION.generateKey(userId + ":" + browserId);

    given(redisUtil.getData(browserSessionKey)).willReturn(previousSessionId);

    // When
    String newSessionId = authSessionService.createSession(userId, browserId);

    // Then
    assertThat(newSessionId).isNotBlank().isNotEqualTo(previousSessionId);

    verify(redisUtil).setDataExpire(
        RedisKeyPolicy.AUTH_SESSION.generateKey(previousSessionId),
        "REVOKED",
        RedisKeyPolicy.AUTH_SESSION.getTtl()
    );
    verify(redisUtil).setDataExpire(
        RedisKeyPolicy.AUTH_SESSION.generateKey(newSessionId),
        "ACTIVE",
        RedisKeyPolicy.AUTH_SESSION.getTtl()
    );
    verify(redisUtil).setDataExpire(
        browserSessionKey,
        newSessionId,
        RedisKeyPolicy.USER_BROWSER_SESSION.getTtl()
    );
  }

  @Test
  @DisplayName("기존 브라우저 세션이 없으면 새 세션만 ACTIVE로 저장한다")
  void create_session_creates_new_session_without_revoking_when_previous_session_absent() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil);
    UUID userId = UUID.randomUUID();
    String browserId = UUID.randomUUID().toString();
    String browserSessionKey = RedisKeyPolicy.USER_BROWSER_SESSION.generateKey(userId + ":" + browserId);

    given(redisUtil.getData(browserSessionKey)).willReturn(null);

    // When
    String sessionId = authSessionService.createSession(userId, browserId);

    // Then
    assertThat(sessionId).isNotBlank();

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(redisUtil).setDataExpire(
        keyCaptor.capture(),
        eq("ACTIVE"),
        eq(RedisKeyPolicy.AUTH_SESSION.getTtl())
    );
    assertThat(keyCaptor.getValue()).isEqualTo(RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId));
    verify(redisUtil, never()).setDataExpire(anyString(), eq("REVOKED"), eq(RedisKeyPolicy.AUTH_SESSION.getTtl()));
  }

  @Test
  @DisplayName("세션 상태가 ACTIVE일 때만 활성 세션으로 판단한다")
  void is_session_active_returns_true_only_when_status_is_active() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil);
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
  @DisplayName("빈 세션 ID는 revoke 하지 않는다")
  void revoke_session_ignores_blank_session_id() {
    // Given
    AuthSessionService authSessionService = new AuthSessionService(redisUtil);

    // When
    authSessionService.revokeSession(null);
    authSessionService.revokeSession(" ");

    // Then
    verify(redisUtil, never()).setDataExpire(anyString(), anyString(), eq(RedisKeyPolicy.AUTH_SESSION.getTtl()));
  }
}
