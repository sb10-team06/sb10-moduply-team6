package com.team6.moduply.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.JwtAuthenticationService;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.handler.JwtLoginSuccessHandler;
import com.team6.moduply.auth.handler.JwtLogoutHandler;
import com.team6.moduply.auth.handler.ModuPlyAccessDeniedHandler;
import com.team6.moduply.auth.handler.ModuPlyAuthenticationEntryPoint;
import com.team6.moduply.auth.handler.ModuPlyLoginFailureHandler;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.config.CsrfConfig;
import com.team6.moduply.common.config.SecurityConfig;
import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.service.FollowService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FollowController.class)
@Import({
    SecurityConfig.class,
    CsrfConfig.class,
    JwtAuthenticationFilter.class,
    ModuPlyAuthenticationEntryPoint.class,
    ModuPlyAccessDeniedHandler.class
})
class FollowControllerSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private FollowService followService;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private JwtAuthenticationService jwtAuthenticationService;

  @MockitoBean
  private ModuPlyLoginFailureHandler moduPlyLoginFailureHandler;

  @MockitoBean
  private JwtLogoutHandler jwtLogoutHandler;

  @MockitoBean
  private JwtLoginSuccessHandler jwtLoginSuccessHandler;

  @Test
  @DisplayName("인증 토큰 없이 팔로우 생성 요청을 보내면 401을 반환한다.")
  void createFollow_fail_without_authentication() throws Exception {
    // given
    FollowRequest request = new FollowRequest(UUID.randomUUID());

    // when & then
    mockMvc.perform(post("/api/follows")
//          .with(user(userDetails(followerId))): 인증 정보 없는 요청
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            // 기대: 401 Unauthorized 반환
        .andExpect(status().isUnauthorized());

    verify(followService, never()).createFollow(any(), any());
  }

  @Test
  @DisplayName("인증 토큰 없이 팔로우 취소 요청을 보내면 401을 반환한다.")
  void cancelFollow_fail_without_authentication() throws Exception {
    // given
    UUID followId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/follows/{followId}", followId)
            .with(csrf()))
        .andExpect(status().isUnauthorized());

    verify(followService, never()).cancelFollow(any(), any());
  }

  @Test
  @DisplayName("유효한 JWT로 팔로우 생성 요청을 보내면 인증 사용자 ID를 서비스에 전달한다.")
  void createFollow_success_with_jwt_authentication() throws Exception {
    // given
    String token = "access-token";
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowRequest request = new FollowRequest(followeeId);
    FollowDto response = new FollowDto(UUID.randomUUID(), followerId, followeeId);

    given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
    given(jwtTokenProvider.getUserId(token)).willReturn(followerId);
    given(jwtAuthenticationService.getAuthentication(followerId))
        .willReturn(authentication(followerId));
    given(followService.createFollow(eq(request), eq(followerId))).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/follows")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    verify(followService).createFollow(request, followerId);
  }

  @Test
  @DisplayName("유효한 JWT로 팔로우 취소 요청을 보내면 인증 사용자 ID를 서비스에 전달한다.")
  void cancelFollow_success_with_jwt_authentication() throws Exception {
    // given
    String token = "access-token";
    UUID followId = UUID.randomUUID();
    UUID followerId = UUID.randomUUID();

    given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
    given(jwtTokenProvider.getUserId(token)).willReturn(followerId);
    given(jwtAuthenticationService.getAuthentication(followerId))
        .willReturn(authentication(followerId));

    // when & then
    mockMvc.perform(delete("/api/follows/{followId}", followId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(followService).cancelFollow(followId, followerId);
  }

  private Authentication authentication(UUID userId) {
    ModuPlyUserDetails userDetails = userDetails(userId);
    return new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );
  }

  private ModuPlyUserDetails userDetails(UUID userId) {
    UserDto userDto = new UserDto(
        userId,
        null,
        "user@example.com",
        "user",
        null,
        Role.USER,
        false
    );
    return new ModuPlyUserDetails(userDto, "encoded-password");
  }
}
