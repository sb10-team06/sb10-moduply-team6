package com.team6.moduply.auth.userdetails;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModuPlyUserDetailsTest {

  @Test
  @DisplayName("일반 로그인 UserDetails는 사용자 정보, 비밀번호, 권한을 반환한다")
  void user_details_returns_default_user_details_values() {
    // Given
    UserDto userDto = userDto(Role.ADMIN, false);
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, "encoded-password");

    // When & Then
    assertThat(userDetails.getUserDto()).isSameAs(userDto);
    assertThat(userDetails.getUsername()).isEqualTo(userDto.getEmail());
    assertThat(userDetails.getName()).isEqualTo(userDto.getEmail());
    assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
    assertThat(userDetails.getAttributes()).isEmpty();
    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_ADMIN");
    assertThat(userDetails.isEnabled()).isTrue();
    assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    assertThat(userDetails.isAccountNonExpired()).isTrue();
    assertThat(userDetails.isAccountNonLocked()).isTrue();
  }

  @Test
  @DisplayName("OAuth2 UserDetails는 attributes를 유지하고 잠금 사용자는 accountNonLocked=false를 반환한다")
  void oauth2_user_details_returns_attributes_and_locked_state() {
    // Given
    UserDto lockedUser = userDto(Role.USER, true);
    Map<String, Object> attributes = Map.of("sub", "oauth-provider-id");
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(lockedUser, attributes);

    // When & Then
    assertThat(userDetails.getPassword()).isEmpty();
    assertThat(userDetails.getAttributes()).isEqualTo(attributes);
    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");
    assertThat(userDetails.isAccountNonLocked()).isFalse();
  }

  private UserDto userDto(Role role, boolean locked) {
    return new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "tester@example.com",
        "tester",
        null,
        role,
        locked
    );
  }
}
