package com.team6.moduply.user;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("비밀번호를 인코딩하여 회원가입 성공")
  public void sign_up_success_with_encoded_password() {
    // Given
    UserCreateRequest request = new UserCreateRequest("tester","test@example.com", "password1!");
    User savedUser = new User("test@example.com", "encoded-password", "tester", Role.USER);
    UserDto expected = new UserDto(null,null,savedUser.getEmail(), savedUser.getName(), null, Role.USER, false);

    given(userRepository.existsByEmail(savedUser.getEmail())).willReturn(false);
    given(userMapper.toDto(any(User.class))).willReturn(expected);
    given(passwordEncoder.encode(request.getPassword())).willReturn(savedUser.getEncodedPassword());
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // When
    UserDto response = userService.createUser(request);

    // Then
    assertThat(response.getEmail()).isEqualTo(expected.getEmail());
    assertThat(response.getRole()).isEqualTo(expected.getRole());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User capturedUser = userCaptor.getValue();
    assertThat(capturedUser.getEmail()).isEqualTo(request.getEmail());
    assertThat(capturedUser.getName()).isEqualTo(request.getName());
    assertThat(capturedUser.getRole()).isEqualTo(Role.USER);
    assertThat(capturedUser.getEncodedPassword()).isEqualTo("encoded-password");
    assertThat(capturedUser.getEncodedPassword()).isNotEqualTo(request.getPassword());

    verify(passwordEncoder).encode(request.getPassword());
  }

  @Test
  @DisplayName("이미 존재하는 이메일이면 회원가입 실패")
  public void sign_up_fail_when_email_already_exists() {
    // Given
    UserCreateRequest request = new UserCreateRequest("tester", "test@example.com", "password1!");

    given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

    // When & Then
    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION);
          assertThat(exception.getDetails().get("email")).isEqualTo(request.getEmail());
        });

    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("동시 회원가입으로 DB 유니크 제약 위반 시 회원가입 실패")
  public void sign_up_fail_when_email_unique_constraint_violated() {
    // Given
    UserCreateRequest request = new UserCreateRequest("tester", "test@example.com", "password1!");

    given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
    given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
    given(userRepository.save(any(User.class)))
        .willThrow(new DataIntegrityViolationException("duplicate email"));

    // When & Then
    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION);
          assertThat(exception.getDetails().get("email")).isEqualTo(request.getEmail());
        });

    verify(passwordEncoder).encode(request.getPassword());
    verify(userRepository).save(any(User.class));
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("사용자 단건 조회 성공")
  public void get_user_success() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);
    UserDto expected = new UserDto(userId, null, user.getEmail(), user.getName(), null, Role.USER,
        false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(expected);

    // When
    UserDto response = userService.getUser(userId);

    // Then
    assertThat(response.getId()).isEqualTo(expected.getId());
    assertThat(response.getEmail()).isEqualTo(expected.getEmail());
    assertThat(response.getName()).isEqualTo(expected.getName());
    assertThat(response.getRole()).isEqualTo(expected.getRole());

    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 단건 조회 실패")
  public void get_user_fail_when_user_not_found() {
    // Given
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.getUser(userId))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository).findById(userId);
    verify(userMapper, never()).toDto(any(User.class));
  }

}
