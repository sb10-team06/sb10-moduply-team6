package com.team6.moduply.auth.userdetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
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

@ExtendWith(MockitoExtension.class)
class ModuPlyUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private RedisUtil redisUtil;

  @Mock
  private BinaryContentService binaryContentService;

  @InjectMocks
  private ModuPlyUserDetailsService userDetailsService;

  @Test
  @DisplayName("Redis에 임시 비밀번호가 있으면 임시 비밀번호를 인증 비밀번호로 사용한다")
  void load_user_by_username_success_with_temp_password() {
    // Given
    String email = "tester@example.com";
    User user = new User(email, "encoded-db-password", "tester", Role.USER);
    UserDto userDto = userDto(email);
    String redisKey = RedisKeyPolicy.PASSWORD_RESET.generateKey(email);

    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    given(binaryContentService.generateUrl(user.getProfileImg())).willReturn(null);
    given(userMapper.toDto(user, null)).willReturn(userDto);
    given(redisUtil.getData(redisKey)).willReturn("encoded-temp-password");

    // When
    ModuPlyUserDetails userDetails = userDetailsService.loadUserByUsername(email);

    // Then
    assertThat(userDetails.getUsername()).isEqualTo(email);
    assertThat(userDetails.getPassword()).isEqualTo("encoded-temp-password");

    verify(userRepository).findByEmail(email);
    verify(binaryContentService).generateUrl(user.getProfileImg());
    verify(userMapper).toDto(user, null);
    verify(redisUtil).getData(redisKey);
  }

  @Test
  @DisplayName("Redis에 임시 비밀번호가 없으면 DB 비밀번호를 인증 비밀번호로 사용한다")
  void load_user_by_username_success_with_db_password() {
    // Given
    String email = "tester@example.com";
    User user = new User(email, "encoded-db-password", "tester", Role.USER);
    UserDto userDto = userDto(email);
    String redisKey = RedisKeyPolicy.PASSWORD_RESET.generateKey(email);

    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    given(binaryContentService.generateUrl(user.getProfileImg())).willReturn(null);
    given(userMapper.toDto(user, null)).willReturn(userDto);
    given(redisUtil.getData(redisKey)).willReturn(null);

    // When
    ModuPlyUserDetails userDetails = userDetailsService.loadUserByUsername(email);

    // Then
    assertThat(userDetails.getUsername()).isEqualTo(email);
    assertThat(userDetails.getPassword()).isEqualTo("encoded-db-password");

    verify(userRepository).findByEmail(email);
    verify(binaryContentService).generateUrl(user.getProfileImg());
    verify(userMapper).toDto(user, null);
    verify(redisUtil).getData(redisKey);
  }

  @Test
  @DisplayName("이메일에 해당하는 사용자가 없으면 사용자명 조회 실패 예외가 발생한다")
  void load_user_by_username_fail_when_user_not_found() {
    // Given
    String email = "unknown@example.com";

    given(userRepository.findByEmail(email)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
        .isInstanceOfSatisfying(AuthException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.USERNAME_NOT_FOUND_EXCEPTION)
        );

    verify(userRepository).findByEmail(email);
  }

  private UserDto userDto(String email) {
    return new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        email,
        "tester",
        null,
        Role.USER,
        false
    );
  }
}
