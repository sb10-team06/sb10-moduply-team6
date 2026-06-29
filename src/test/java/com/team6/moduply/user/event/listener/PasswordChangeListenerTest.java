package com.team6.moduply.user.event.listener;

import static org.mockito.Mockito.verify;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.event.PasswordChangeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordChangeListenerTest {

  @Mock
  private RedisUtil redisUtil;

  @InjectMocks
  private PasswordChangeListener passwordChangeListener;

  @Test
  @DisplayName("비밀번호 변경 이벤트 처리 시 Redis 임시 비밀번호를 즉시 삭제한다")
  void handle_password_change_event_success() {
    // Given
    String email = "tester@example.com";

    // When
    passwordChangeListener.handlePasswordChangeEvent(new PasswordChangeEvent(email));

    // Then
    verify(redisUtil).deleteData(RedisKeyPolicy.PASSWORD_RESET.generateKey(email));
  }
}
