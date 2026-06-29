package com.team6.moduply.user.event.listener;

import static org.mockito.Mockito.verify;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.event.PasswordChangeEvent;
import com.team6.moduply.user.event.UserLockedEvent;
import com.team6.moduply.user.event.UserUnlockedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSecurityEventListenerTest {

  @Mock
  private RedisUtil redisUtil;

  @InjectMocks
  private UserSecurityEventListener userSecurityEventListener;

  @Test
  @DisplayName("비밀번호 변경 이벤트 처리 시 Redis 임시 비밀번호를 즉시 삭제한다")
  void handle_password_change_event_success() {
    // Given
    String email = "tester@example.com";

    // When
    userSecurityEventListener.handlePasswordChangeEvent(new PasswordChangeEvent(email));

    // Then
    verify(redisUtil).deleteData(RedisKeyPolicy.PASSWORD_RESET.generateKey(email));
  }

  @Test
  @DisplayName("사용자 잠금 이벤트 처리 시 Redis 잠금 블랙리스트를 등록한다")
  void handle_user_locked_event_success() {
    // Given
    String email = "tester@example.com";

    // When
    userSecurityEventListener.handlerUserLockedEvent(new UserLockedEvent(email));

    // Then
    verify(redisUtil).setDataExpire(
        RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(email),
        "locked",
        RedisKeyPolicy.BLACKLIST_LOCKED.getTtl()
    );
  }

  @Test
  @DisplayName("사용자 잠금 해제 이벤트 처리 시 Redis 잠금 블랙리스트를 삭제한다")
  void handle_user_unlocked_event_success() {
    // Given
    String email = "tester@example.com";

    // When
    userSecurityEventListener.handlerUserUnlockedEvent(new UserUnlockedEvent(email));

    // Then
    verify(redisUtil).deleteData(RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(email));
  }
}
