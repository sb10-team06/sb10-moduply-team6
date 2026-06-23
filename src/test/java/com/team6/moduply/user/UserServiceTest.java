package com.team6.moduply.user;


import static org.mockito.BDDMockito.given;

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
  @DisplayName("회원가입 성공")
  public void sign_up_success() {
    // Given
    UserCreateRequest request = new UserCreateRequest("test@example.com", "tester", "password1!");
    User savedUser = new User("test@example.com", "password1!", "tester", Role.USER);
    UserDto expected = new UserDto(null,null,savedUser.getEmail(), savedUser.getName(), null, Role.USER, false);

    given(userRepository.existsByEmail(savedUser.getEmail())).willReturn(true);

  }
}
