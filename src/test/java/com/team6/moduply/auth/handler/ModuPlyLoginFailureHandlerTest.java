package com.team6.moduply.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ModuPlyLoginFailureHandlerTest {

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 401 공통 에러 응답을 반환한다")
  void login_fail_when_bad_credentials() throws Exception {
    // Given
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    ModuPlyLoginFailureHandler handler = new ModuPlyLoginFailureHandler(objectMapper);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad credentials"));

    // Then
    JsonNode responseBody = objectMapper.readTree(response.getContentAsString());

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(responseBody.get("code").asText()).isEqualTo("auth02");
    assertThat(responseBody.get("message").asText()).isEqualTo("이메일 또는 비밀번호가 일치하지 않습니다.");
  }

  @Test
  @DisplayName("잠긴 계정이면 403 공통 에러 응답을 반환한다")
  void login_fail_when_account_is_locked() throws Exception {
    // Given
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    ModuPlyLoginFailureHandler handler = new ModuPlyLoginFailureHandler(objectMapper);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    handler.onAuthenticationFailure(request, response, new LockedException("locked"));

    // Then
    JsonNode responseBody = objectMapper.readTree(response.getContentAsString());

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(responseBody.get("code").asText()).isEqualTo("auth03");
    assertThat(responseBody.get("message").asText()).isEqualTo("잠긴 계정입니다.");
  }
}
