package com.team6.moduply.sse;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.config.support.IntegrationTestSupport;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SseIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SseEmitterManager sseEmitterManager;

  private static final UUID TEST_USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    UserDto userDto = new UserDto(
        TEST_USER_ID, Instant.now(), "test@test.com",
        "테스트", null, Role.USER, false
    );
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, "password");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("SSE 연결 요청 시 200과 text/event-stream 헤더를 반환한다.")
  void subscribe_success() throws Exception {
    mockMvc.perform(get("/api/sse"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/event-stream"));
  }

  @Test
  @DisplayName("SSE 연결 후 이벤트를 전송하면 예외가 발생하지 않는다.")
  void send_after_connect() throws Exception {
    // given
    mockMvc.perform(get("/api/sse"))
        .andExpect(status().isOk());

    // when & then
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> sseEmitterManager.send(TEST_USER_ID, "test message")
    );
  }

  @Test
  @DisplayName("인증 없이 SSE 연결 시 401을 반환한다.")
  void subscribe_fail_with_no_auth() throws Exception {
    SecurityContextHolder.clearContext();

    mockMvc.perform(get("/api/sse"))
        .andExpect(status().isUnauthorized());
  }
}