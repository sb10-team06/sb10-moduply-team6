package com.team6.moduply.auth.util;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import java.util.Collections;
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
class RefreshTokenRedisUtilTest {

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @InjectMocks
  private RefreshTokenRedisUtil refreshTokenRedisUtil;

  @Test
  @DisplayName("refresh token 원자 갱신은 Lua 스크립트 실행으로 위임된다")
  void rotate_success() {
    // Given
    String email = "tester@example.com";
    String presentedRefreshToken = "refresh-token";
    String newRefreshToken = "new-refresh-token";

    // When
    refreshTokenRedisUtil.rotate(email, presentedRefreshToken, newRefreshToken);

    // Then
    verify(stringRedisTemplate).execute(
        eq(getRotateScript()),
        eq(Collections.singletonList(RedisKeyPolicy.REFRESH_TOKEN.generateKey(email))),
        eq(presentedRefreshToken),
        eq(newRefreshToken),
        eq(String.valueOf(RedisKeyPolicy.REFRESH_TOKEN.getTtl().toMillis()))
    );
  }

  @SuppressWarnings("unchecked")
  private DefaultRedisScript<String> getRotateScript() {
    return (DefaultRedisScript<String>) ReflectionTestUtils
        .getField(RefreshTokenRedisUtil.class, "ROTATE_REFRESH_TOKEN_SCRIPT");
  }
}
