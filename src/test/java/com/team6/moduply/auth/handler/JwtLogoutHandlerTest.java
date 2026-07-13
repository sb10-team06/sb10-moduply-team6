package com.team6.moduply.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.event.UserLogoutEvent;
import com.team6.moduply.auth.handler.logout.JwtLogoutHandler;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtLogoutHandlerTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RedisUtil redisUtil;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @Mock
  private AuthService authService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("로그아웃 시 Refresh Token 쿠키를 만료시키고 SecurityContext를 비운다")
  void logout_success() {
    // Given
    JwtLogoutHandler logoutHandler = new JwtLogoutHandler(jwtTokenProvider, redisUtil,
        applicationEventPublisher, authService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("REFRESH_TOKEN", "refresh-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("tester", null, "ROLE_USER");

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String sessionId = UUID.randomUUID().toString();
    given(jwtTokenProvider.getSessionId("refresh-token")).willReturn(sessionId);
    given(jwtTokenProvider.getEmail("refresh-token")).willReturn("tester@example.com");
    UUID userId = UUID.randomUUID();
    given(jwtTokenProvider.getUserId("refresh-token")).willReturn(userId);
    // When
    logoutHandler.logout(request, response, authentication);

    // Then
    Cookie deleteCookie = response.getCookie("REFRESH_TOKEN");

    assertThat(deleteCookie).isNotNull();
    assertThat(deleteCookie.getValue()).isNull();
    assertThat(deleteCookie.getMaxAge()).isZero();
    assertThat(deleteCookie.getPath()).isEqualTo("/");
    assertThat(deleteCookie.isHttpOnly()).isTrue();
    assertThat(deleteCookie.getSecure()).isTrue();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(jwtTokenProvider).getEmail("refresh-token");
    verify(jwtTokenProvider).getUserId("refresh-token");
    verify(redisUtil).deleteData(RedisKeyPolicy.REFRESH_TOKEN.generateKey(sessionId));
    verify(authService).revokeSession(sessionId);
    ArgumentCaptor<UserLogoutEvent> eventCaptor = ArgumentCaptor.forClass(UserLogoutEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    UserLogoutEvent capturedEvent = eventCaptor.getValue();
    assertThat(capturedEvent.email()).isEqualTo("tester@example.com");
    assertThat(capturedEvent.userId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("로그아웃 시 남은 만료시간 동안 Access Token을 블랙리스트에 등록한다")
  void logout_blacklists_access_token() {
    // Given
    JwtLogoutHandler logoutHandler = new JwtLogoutHandler(jwtTokenProvider, redisUtil,
        applicationEventPublisher, authService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
    request.setCookies(new Cookie("REFRESH_TOKEN", "refresh-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("tester", null, "ROLE_USER");
    Duration remainingTtl = Duration.ofMinutes(10);
    String sessionId = UUID.randomUUID().toString();

    given(jwtTokenProvider.validateAccessToken("access-token")).willReturn(true);
    given(jwtTokenProvider.getRemainingExpiration("access-token")).willReturn(remainingTtl);
    given(jwtTokenProvider.getSessionId("access-token")).willReturn(sessionId);
    given(jwtTokenProvider.getEmail("refresh-token")).willReturn("tester@example.com");

    // When
    logoutHandler.logout(request, response, authentication);

    // Then
    verify(authService).blacklistAccessToken("access-token", remainingTtl);
    verify(redisUtil).deleteData(RedisKeyPolicy.REFRESH_TOKEN.generateKey(sessionId));
    verify(authService).revokeSession(sessionId);
  }
}
