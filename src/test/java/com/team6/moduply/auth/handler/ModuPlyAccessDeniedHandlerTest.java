package com.team6.moduply.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ModuPlyAccessDeniedHandlerTest {

  @Test
  @DisplayName("인가 실패 시 공통 에러 응답을 반환한다")
  void access_denied() throws Exception {
    // Given
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    ModuPlyAccessDeniedHandler handler = new ModuPlyAccessDeniedHandler(objectMapper);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    handler.handle(request, response, new AccessDeniedException("forbidden"));

    // Then
    JsonNode responseBody = objectMapper.readTree(response.getContentAsString());

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(responseBody.get("code").asText()).isEqualTo("auth04");
    assertThat(responseBody.get("message").asText()).isEqualTo("접근 권한이 없습니다.");
  }
}
