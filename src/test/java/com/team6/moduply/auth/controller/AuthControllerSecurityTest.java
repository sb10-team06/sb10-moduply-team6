package com.team6.moduply.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.dto.JwtDto;
import com.team6.moduply.auth.dto.TokenRefreshDto;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.handler.csrf.SpaCsrfTokenRequestHandler;
import com.team6.moduply.auth.handler.exception.ModuPlyAccessDeniedHandler;
import com.team6.moduply.auth.handler.exception.ModuPlyAuthenticationEntryPoint;
import com.team6.moduply.auth.handler.login.JwtLoginSuccessHandler;
import com.team6.moduply.auth.handler.login.ModuPlyLoginFailureHandler;
import com.team6.moduply.auth.handler.logout.JwtLogoutHandler;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.common.config.CsrfConfig;
import com.team6.moduply.common.config.SecurityConfig;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({
    SecurityConfig.class,
    CsrfConfig.class,
    SpaCsrfTokenRequestHandler.class,
    ModuPlyAccessDeniedHandler.class,
    ModuPlyAuthenticationEntryPoint.class
})
class AuthControllerSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockitoBean
  private ModuPlyLoginFailureHandler moduPlyLoginFailureHandler;

  @MockitoBean
  private JwtLogoutHandler jwtLogoutHandler;

  @MockitoBean
  private JwtLoginSuccessHandler jwtLoginSuccessHandler;

  @BeforeEach
  void passThroughJwtFilter() throws Exception {
    doAnswer(invocation -> {
      jakarta.servlet.FilterChain chain = invocation.getArgument(2);
      chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(jwtAuthenticationFilter).doFilter(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    );
  }

  @Test
  @DisplayName("CSRF 토큰 없이 Refresh Token 재발급을 요청하면 403을 반환한다.")
  void refresh_fail_without_csrf_token() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie("REFRESH_TOKEN", "refresh-token")))
        .andExpect(status().isForbidden());

    verify(authService, never()).refreshTokens("refresh-token");
  }

  @Test
  @DisplayName("유효한 CSRF 토큰과 함께 Refresh Token 재발급을 요청하면 성공한다.")
  void refresh_success_with_csrf_token() throws Exception {
    String refreshToken = "refresh-token";
    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "tester@example.com",
        "tester",
        null,
        Role.USER,
        false
    );
    given(authService.refreshTokens(refreshToken)).willReturn(
        new TokenRefreshDto(new JwtDto(userDto, "new-access-token"), "new-refresh-token")
    );

    mockMvc.perform(post("/api/auth/refresh")
            .cookie(new Cookie("REFRESH_TOKEN", refreshToken))
            .with(csrf()))
        .andExpect(status().isOk());

    verify(authService).refreshTokens(refreshToken);
  }
}
