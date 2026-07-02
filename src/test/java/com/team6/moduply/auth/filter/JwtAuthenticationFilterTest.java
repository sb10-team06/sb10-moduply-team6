package com.team6.moduply.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.handler.exception.ModuPlyAuthenticationEntryPoint;
import com.team6.moduply.common.util.RedisUtil;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private AuthService authService;

  @Mock
  private RedisUtil redisUtil;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("유효한 Access Token이면 SecurityContext에 인증 정보를 저장한다")
  void do_filter_success_when_access_token_is_valid() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    String token = "valid-token";
    Authentication authentication = new TestingAuthenticationToken("tester", null, "ROLE_USER");
    ModuPlyAuthenticationEntryPoint entryPoint =
        new ModuPlyAuthenticationEntryPoint(objectMapper());
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(jwtTokenProvider, authService, entryPoint, redisUtil);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
    given(jwtTokenProvider.getUserId(token)).willReturn(userId);
    given(authService.getAuthentication(userId)).willReturn(authentication);

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    assertThat(response.getStatus()).isEqualTo(200);

    verify(jwtTokenProvider).validateAccessToken(token);
    verify(jwtTokenProvider).getUserId(token);
    verify(authService).getAuthentication(userId);
  }

  @Test
  @DisplayName("Access Token이 없으면 인증 시도 없이 다음 필터로 넘긴다")
  void do_filter_skip_when_access_token_is_missing() throws Exception {
    // Given
    ModuPlyAuthenticationEntryPoint entryPoint =
        new ModuPlyAuthenticationEntryPoint(objectMapper());
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(jwtTokenProvider, authService, entryPoint, redisUtil);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getStatus()).isEqualTo(200);

    verify(jwtTokenProvider, never()).validateAccessToken(any());
    verify(authService, never()).getAuthentication(any());
  }

  @Test
  @DisplayName("유효하지 않은 Access Token이면 401 응답을 반환하고 SecurityContext를 비운다")
  void do_filter_fail_when_access_token_is_invalid() throws Exception {
    // Given
    String token = "invalid-token";
    ModuPlyAuthenticationEntryPoint entryPoint =
        new ModuPlyAuthenticationEntryPoint(objectMapper());
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(jwtTokenProvider, authService, entryPoint, redisUtil);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    given(jwtTokenProvider.validateAccessToken(token)).willReturn(false);

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("auth06");

    verify(jwtTokenProvider).validateAccessToken(token);
    verify(jwtTokenProvider, never()).getUserId(token);
    verify(authService, never()).getAuthentication(any());
  }

  private ObjectMapper objectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }
}
