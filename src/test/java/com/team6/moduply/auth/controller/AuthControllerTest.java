package com.team6.moduply.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.dto.ResetPasswordRequest;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @Test
  @DisplayName("비밀번호 재발급 요청 형식이 정상이라면 204를 반환한다")
  void reset_password_success() throws Exception {
    // Given
    ResetPasswordRequest request = resetPasswordRequest("tester@example.com");

    // When & Then
    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(authService).resetPassword(any(ResetPasswordRequest.class));
  }

  @Test
  @DisplayName("존재하지 않는 이메일이어도 요청 형식이 정상이라면 204를 반환한다")
  void reset_password_success_when_user_not_found() throws Exception {
    // Given
    ResetPasswordRequest request = resetPasswordRequest("unknown@example.com");

    // When & Then
    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(authService).resetPassword(any(ResetPasswordRequest.class));
  }

  @Test
  @DisplayName("비밀번호 재발급 요청 이메일 형식이 올바르지 않으면 400을 반환한다")
  void reset_password_fail_when_email_is_invalid() throws Exception {
    // Given
    ResetPasswordRequest request = resetPasswordRequest("invalid-email");

    // When & Then
    mockMvc.perform(post("/api/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
  }

  private ResetPasswordRequest resetPasswordRequest(String email) {
    ResetPasswordRequest request = new ResetPasswordRequest();
    ReflectionTestUtils.setField(request, "email", email);
    return request;
  }
}
