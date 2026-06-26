package com.team6.moduply.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtLogoutHandlerTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("로그아웃 시 Refresh Token 쿠키를 만료시키고 SecurityContext를 비운다")
  void logout_success() {
    // Given
    JwtLogoutHandler logoutHandler = new JwtLogoutHandler();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("tester", null, "ROLE_USER");

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // When
    logoutHandler.logout(request, response, authentication);

    // Then
    Cookie deleteCookie = response.getCookie("REFRESH_TOKEN");

    assertThat(deleteCookie).isNotNull();
    assertThat(deleteCookie.getValue()).isNull();
    assertThat(deleteCookie.getMaxAge()).isZero();
    assertThat(deleteCookie.getPath()).isEqualTo("/");
    assertThat(deleteCookie.isHttpOnly()).isTrue();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
