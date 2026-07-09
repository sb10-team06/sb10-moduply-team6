package com.team6.moduply.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.oauth2.handler.OAuth2SuccessHandler;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RedisUtil redisUtil;

  @Test
  @DisplayName("OAuth2 로그인 성공 시 Refresh Token을 쿠키와 Redis에 저장하고 프론트로 리다이렉트한다")
  void oauth2_login_success() throws Exception {
    // Given
    OAuth2SuccessHandler handler = new OAuth2SuccessHandler(jwtTokenProvider, redisUtil);
    ReflectionTestUtils.setField(handler, "refreshTokenExpirationMinutes", 420);
    ReflectionTestUtils.setField(handler, "redirectUrl", "http://localhost:8080/#/contents");

    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "tester@example.com",
        "tester",
        null,
        Role.USER,
        false
    );
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, "encoded-password");
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );

    given(jwtTokenProvider.generateRefreshToken(authentication)).willReturn("refresh-token");

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    handler.onAuthenticationSuccess(request, response, authentication);

    // Then
    String setCookie = response.getHeader("Set-Cookie");

    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:8080/#/contents");
    assertThat(setCookie).contains("REFRESH_TOKEN=refresh-token");
    assertThat(setCookie).contains("HttpOnly");
    assertThat(setCookie).doesNotContain("Secure");
    assertThat(setCookie).contains("Path=/");
    assertThat(setCookie).contains("Max-Age=25200");
    assertThat(response.getRedirectedUrl()).doesNotContain("access");

    verify(jwtTokenProvider, never()).generateAccessToken(authentication);
    verify(redisUtil).setDataExpire(
        RedisKeyPolicy.REFRESH_TOKEN.generateKey("tester@example.com"),
        "refresh-token",
        RedisKeyPolicy.REFRESH_TOKEN.getTtl()
    );
  }
}
