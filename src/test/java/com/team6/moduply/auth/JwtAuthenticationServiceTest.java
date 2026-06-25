package com.team6.moduply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationServiceTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private JwtAuthenticationService jwtAuthenticationService;

  @Test
  @DisplayName("사용자 ID로 최신 사용자 상태를 조회해 인증 객체를 생성한다")
  void get_authentication_success() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = new User("tester@example.com", "encoded-password", "tester", Role.USER);
    UserDto userDto = new UserDto(
        userId,
        Instant.now(),
        user.getEmail(),
        user.getName(),
        null,
        Role.USER,
        false
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(userDto);

    // When
    Authentication authentication = jwtAuthenticationService.getAuthentication(userId);

    // Then
    ModuPlyUserDetails principal = (ModuPlyUserDetails) authentication.getPrincipal();

    assertThat(principal.getUserdto().getId()).isEqualTo(userId);
    assertThat(principal.getUsername()).isEqualTo(user.getEmail());
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");

    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("토큰의 사용자 ID가 DB에 없으면 유효하지 않은 토큰 예외가 발생한다")
  void get_authentication_fail_when_user_not_found() {
    // Given
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(userId))
        .isInstanceOfSatisfying(AuthException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN_EXCEPTION)
        );

    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("잠긴 계정이면 인증 객체 생성에 실패한다")
  void get_authentication_fail_when_account_is_locked() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = new User("tester@example.com", "encoded-password", "tester", Role.USER);
    UserDto lockedUserDto = new UserDto(
        userId,
        Instant.now(),
        user.getEmail(),
        user.getName(),
        null,
        Role.USER,
        true
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(lockedUserDto);

    // When & Then
    assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(userId))
        .isInstanceOfSatisfying(AuthException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION)
        );

    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user);
  }
}
