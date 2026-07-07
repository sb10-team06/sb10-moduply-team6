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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
    sseEmitterManager.removeAllEmitters(TEST_USER_ID);
  }

  @Test
  @DisplayName("SSE 연결 요청 시 200과 text/event-stream 헤더를 반환한다.")
  void subscribe_success() throws Exception {
    mockMvc.perform(get("/api/sse"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/event-stream"));

    // 테스트 후 즉시 연결 종료
    sseEmitterManager.removeAllEmitters(TEST_USER_ID);
  }
}