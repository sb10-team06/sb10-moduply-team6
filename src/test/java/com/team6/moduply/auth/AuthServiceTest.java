package com.team6.moduply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.dto.ResetPasswordRequest;
import com.team6.moduply.auth.event.TempPasswordEvent;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.TempPasswordUtil;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @Mock
  private TempPasswordUtil tempPasswordUtil;

  @Mock
  private BinaryContentService binaryContentService;

  @InjectMocks
  private AuthService authService;

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
    given(binaryContentService.generateUrl(user.getProfileImg())).willReturn(null);
    given(userMapper.toDto(user, null)).willReturn(userDto);

    // When
    Authentication authentication = authService.getAuthentication(userId);

    // Then
    ModuPlyUserDetails principal = (ModuPlyUserDetails) authentication.getPrincipal();

    assertThat(principal.getUserDto().getId()).isEqualTo(userId);
    assertThat(principal.getUsername()).isEqualTo(user.getEmail());
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");

    verify(userRepository).findById(userId);
    verify(binaryContentService).generateUrl(user.getProfileImg());
    verify(userMapper).toDto(user, null);
  }

  @Test
  @DisplayName("토큰의 사용자 ID가 DB에 없으면 유효하지 않은 토큰 예외가 발생한다")
  void get_authentication_fail_when_user_not_found() {
    // Given
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> authService.getAuthentication(userId))
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
    given(binaryContentService.generateUrl(user.getProfileImg())).willReturn(null);
    given(userMapper.toDto(user, null)).willReturn(lockedUserDto);

    // When & Then
    assertThatThrownBy(() -> authService.getAuthentication(userId))
        .isInstanceOfSatisfying(AuthException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION)
        );

    verify(userRepository).findById(userId);
    verify(binaryContentService).generateUrl(user.getProfileImg());
    verify(userMapper).toDto(user, null);
  }

  @Test
  @DisplayName("비밀번호 재발급 요청 시 임시 비밀번호와 만료시각을 담은 메일 이벤트를 발행한다")
  void reset_password_success() {
    // Given
    ResetPasswordRequest request = resetPasswordRequest("tester@example.com");
    String tempPassword = "Temp1234!";
    Instant beforeRequest = Instant.now();

    given(userRepository.existsByEmail(request.getEmail())).willReturn(true);
    given(tempPasswordUtil.generate(8)).willReturn(tempPassword);

    // When
    authService.resetPassword(request);

    // Then
    verify(userRepository).existsByEmail(request.getEmail());
    verify(tempPasswordUtil).generate(8);

    ArgumentCaptor<TempPasswordEvent> eventCaptor = ArgumentCaptor.forClass(
        TempPasswordEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

    TempPasswordEvent event = eventCaptor.getValue();
    assertThat(event.getEmail()).isEqualTo(request.getEmail());
    assertThat(event.getTempPassword()).isEqualTo(tempPassword);
    assertThat(event.getExpiresAt())
        .isBetween(
            beforeRequest.plus(RedisKeyPolicy.PASSWORD_RESET.getTtl()),
            Instant.now().plus(RedisKeyPolicy.PASSWORD_RESET.getTtl())
        );
  }

  @Test
  @DisplayName("비밀번호 재발급 요청 이메일이 존재하지 않으면 아무 작업 없이 종료한다")
  void reset_password_noop_when_user_not_found() {
    // Given
    ResetPasswordRequest request = resetPasswordRequest("unknown@example.com");

    given(userRepository.existsByEmail(request.getEmail())).willReturn(false);

    // When
    authService.resetPassword(request);

    // Then
    verify(userRepository).existsByEmail(request.getEmail());
    verify(tempPasswordUtil, never()).generate(8);
    verify(applicationEventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }

  private ResetPasswordRequest resetPasswordRequest(String email) {
    ResetPasswordRequest request = new ResetPasswordRequest();
    ReflectionTestUtils.setField(request, "email", email);
    return request;
  }
}
