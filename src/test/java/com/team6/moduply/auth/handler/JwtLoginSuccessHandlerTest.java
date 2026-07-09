package com.team6.moduply.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.handler.login.JwtLoginSuccessHandler;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtLoginSuccessHandlerTest {
  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RedisUtil redisUtil;

  @Test
  @DisplayName("로그인 성공 시 Access Token은 헤더와 응답 바디에, Refresh Token은 쿠키에 담는다")
  void login_success() throws Exception {
    // Given
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    JwtLoginSuccessHandler handler = new JwtLoginSuccessHandler(
        jwtTokenProvider,
        objectMapper,
        CookieCsrfTokenRepository.withHttpOnlyFalse(),
        redisUtil
    );

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

    String versionKey = RedisKeyPolicy.USER_TOKEN_VERSION.generateKey(userDto.getEmail());
    given(redisUtil.getData(versionKey)).willReturn("0");
    given(jwtTokenProvider.generateAccessToken(authentication, 0L)).willReturn("access-token");
    given(jwtTokenProvider.generateRefreshToken(authentication)).willReturn("refresh-token");
    given(jwtTokenProvider.getEmail("refresh-token")).willReturn("tester@example.com");

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    handler.onAuthenticationSuccess(request, response, authentication);

    // Then
    Cookie refreshTokenCookie = response.getCookie("REFRESH_TOKEN");
    Cookie csrfTokenCookie = response.getCookie("XSRF-TOKEN");
    JsonNode responseBody = objectMapper.readTree(response.getContentAsString());

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access-token");
    assertThat(refreshTokenCookie).isNotNull();
    assertThat(refreshTokenCookie.getValue()).isEqualTo("refresh-token");
    assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
    assertThat(refreshTokenCookie.getPath()).isEqualTo("/");
    assertThat(csrfTokenCookie).isNotNull();
    assertThat(csrfTokenCookie.isHttpOnly()).isFalse();
    assertThat(csrfTokenCookie.getPath()).isEqualTo("/");
    assertThat(responseBody.get("accessToken").asText()).isEqualTo("access-token");
    assertThat(responseBody.get("userDto").get("email").asText()).isEqualTo("tester@example.com");
    verify(redisUtil).setDataExpire(
        RedisKeyPolicy.REFRESH_TOKEN.generateKey("tester@example.com"),
        "refresh-token",
        RedisKeyPolicy.REFRESH_TOKEN.getTtl()
    );
    verify(redisUtil).setDataIfAbsent(versionKey, "0");
  }
}
