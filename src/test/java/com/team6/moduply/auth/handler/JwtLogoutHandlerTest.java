package com.team6.moduply.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.event.UserLogoutEvent;
import com.team6.moduply.auth.handler.logout.JwtLogoutHandler;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("로그아웃 시 Refresh Token 쿠키를 만료시키고 SecurityContext를 비운다")
  void logout_success() {
    // Given
    JwtLogoutHandler logoutHandler = new JwtLogoutHandler(jwtTokenProvider, redisUtil,
        applicationEventPublisher);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("REFRESH_TOKEN", "refresh-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("tester", null, "ROLE_USER");

    SecurityContextHolder.getContext().setAuthentication(authentication);
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
    assertThat(deleteCookie.getSecure()).isFalse();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(jwtTokenProvider).getEmail("refresh-token");
    verify(jwtTokenProvider).getUserId("refresh-token");
    verify(redisUtil).deleteData(RedisKeyPolicy.REFRESH_TOKEN.generateKey("tester@example.com"));
    ArgumentCaptor<UserLogoutEvent> eventCaptor = ArgumentCaptor.forClass(UserLogoutEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    UserLogoutEvent capturedEvent = eventCaptor.getValue();
    assertEquals("tester@example.com", capturedEvent.email());
    assertEquals(userId, capturedEvent.userId());
  }
}
