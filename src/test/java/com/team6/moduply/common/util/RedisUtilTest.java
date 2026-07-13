package com.team6.moduply.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisUtilTest {

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private RedisUtil redisUtil;

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

  @Test
  @DisplayName("Redis 데이터가 없으면 값을 저장한다")
  void set_data_if_absent_success() {
    // Given
    String key = "token-version:tester@example.com";
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(key, "0")).thenReturn(true);

    // When
    boolean result = redisUtil.setDataIfAbsent(key, "0");

    // Then
    assertThat(result).isTrue();
    verify(valueOperations).setIfAbsent(key, "0");
  }

  @Test
  @DisplayName("Redis 조건부 저장 재시도 실패 시 예외를 다시 던진다")
  void recover_set_data_if_absent_throws_exception() {
    // Given
    String key = "token-version:tester@example.com";
    DataAccessResourceFailureException exception =
        new DataAccessResourceFailureException("redis unavailable");

    // When & Then
    assertThatThrownBy(() -> redisUtil.recoverSetDataIfAbsent(exception, key, "0"))
        .isSameAs(exception);
  }

  @Test
  @DisplayName("Redis 값을 원자적으로 증가시킨다")
  void increment_success() {
    // Given
    String key = "token-version:tester@example.com";
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(key)).thenReturn(2L);

    // When
    long result = redisUtil.increment(key);

    // Then
    assertThat(result).isEqualTo(2L);
    verify(valueOperations).increment(key);
  }

  @Test
  @DisplayName("Redis 증가 재시도 실패 시 예외를 다시 던진다")
  void recover_increment_throws_exception() {
    // Given
    String key = "token-version:tester@example.com";
    DataAccessResourceFailureException exception =
        new DataAccessResourceFailureException("redis unavailable");

    // When & Then
    assertThatThrownBy(() -> redisUtil.recoverIncrement(exception, key))
        .isSameAs(exception);
  }
}
