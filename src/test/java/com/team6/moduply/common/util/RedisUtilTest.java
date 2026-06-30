package com.team6.moduply.common.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class RedisUtilTest {

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @InjectMocks
  private RedisUtil redisUtil;

  @Test
  @DisplayName("refresh token 원자 갱신은 Lua 스크립트 실행으로 위임된다")
  void rotate_refresh_token_success() {
    // Given
    String key = "refresh:tester@example.com";
    String presentedRefreshToken = "refresh-token";
    String newRefreshToken = "new-refresh-token";
    Duration ttl = Duration.ofHours(7);

    // When
    redisUtil.rotateRefreshToken(key, presentedRefreshToken, newRefreshToken, ttl);

    // Then
    verify(stringRedisTemplate).execute(
        eq(getRotateScript()),
        eq(Collections.singletonList(key)),
        eq(presentedRefreshToken),
        eq(newRefreshToken),
        eq(String.valueOf(ttl.toMillis()))
    );
  }

  @Test
  @DisplayName("Redis 데이터 삭제 요청 시 지정한 키를 삭제한다")
  void delete_data_success() {
    // Given
    String key = "reset:tester@example.com";

    // When
    redisUtil.deleteData(key);

    // Then
    verify(stringRedisTemplate).delete(key);
  }

  @Test
  @DisplayName("Redis 데이터 삭제 재시도 실패 시 예외를 다시 던진다")
  void recover_delete_data_throws_exception() {
    // Given
    String key = "reset:tester@example.com";
    DataAccessResourceFailureException exception =
        new DataAccessResourceFailureException("redis unavailable");

    // When & Then
    assertThatThrownBy(() -> redisUtil.recoverDeleteData(exception, key))
        .isSameAs(exception);
  }

  @SuppressWarnings("unchecked")
  private DefaultRedisScript<String> getRotateScript() {
    return (DefaultRedisScript<String>) org.springframework.test.util.ReflectionTestUtils
        .getField(RedisUtil.class, "ROTATE_REFRESH_TOKEN_SCRIPT");
  }
}
