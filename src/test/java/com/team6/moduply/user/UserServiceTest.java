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
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("이미 존재하는 이메일입니다.");

    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

}
