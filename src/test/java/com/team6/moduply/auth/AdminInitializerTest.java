package com.team6.moduply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Test
  @DisplayName("초기 관리자 이메일 사용자가 없으면 ADMIN 계정을 생성한다")
  void init_admin_creates_admin_when_email_not_exists() {
    // Given
    AdminInitializer initializer = initializer();
    given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.empty());
    given(passwordEncoder.encode("admin-password")).willReturn("encoded-admin-password");

    // When
    initializer.initAdmin();

    // Then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getEmail()).isEqualTo("admin@example.com");
    assertThat(savedUser.getEncodedPassword()).isEqualTo("encoded-admin-password");
    assertThat(savedUser.getName()).isEqualTo("Admin");
    assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
  }

  @Test
  @DisplayName("초기 관리자 이메일 사용자가 이미 ADMIN이면 아무 작업도 하지 않는다")
  void init_admin_skips_when_existing_user_is_admin() {
    // Given
    AdminInitializer initializer = initializer();
    User admin = new User("admin@example.com", "encoded-password", "Admin", Role.ADMIN);
    given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));

    // When
    initializer.initAdmin();

    // Then
    verify(userRepository, never()).save(any(User.class));
    verify(passwordEncoder, never()).encode(any(String.class));
  }

  @Test
  @DisplayName("초기 관리자 이메일을 일반 사용자가 사용 중이면 예외를 던진다")
  void init_admin_throws_when_email_used_by_non_admin_user() {
    // Given
    AdminInitializer initializer = initializer();
    User user = new User("admin@example.com", "encoded-password", "user", Role.USER);
    given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(user));

    // When & Then
    assertThatThrownBy(initializer::initAdmin)
        .isInstanceOfSatisfying(UserException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.ADMIN_EMAIL_ALREADY_USED_EXCEPTION)
        );

    verify(userRepository, never()).save(any(User.class));
  }

  private AdminInitializer initializer() {
    AdminInitializer initializer = new AdminInitializer(userRepository, passwordEncoder);
    ReflectionTestUtils.setField(initializer, "adminEmail", "admin@example.com");
    ReflectionTestUtils.setField(initializer, "adminPassword", "admin-password");
    return initializer;
  }
}
