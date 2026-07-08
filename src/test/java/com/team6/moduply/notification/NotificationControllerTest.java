package com.team6.moduply.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.notification.controller.NotificationController;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.dto.NotificationSortBy;
import com.team6.moduply.notification.exception.NotificationErrorCode;
import com.team6.moduply.notification.exception.NotificationException;
import com.team6.moduply.notification.service.NotificationService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = NotificationController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private NotificationService notificationService;

  private static final UUID TEST_RECEIVER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    UserDto userDto = new UserDto(
        TEST_RECEIVER_ID, Instant.now(), "test@test.com",
        "테스트", null, Role.USER, false
    );
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, "password");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("알림 목록을 조회하면 200을 반환한다.")
  void getNotifications_success() throws Exception {
    // given
    CursorResponse<NotificationDto> response = new CursorResponse<>(
        List.of(), null, null, false, 0L, "createdAt", SortDirection.DESCENDING
    );

    given(notificationService.findAll(any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/notifications")
            .param("limit", "10")
            .param("sortDirection", "DESCENDING")
            .param("sortBy", "createdAt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(0));
  }

  @Test
  @DisplayName("limit 없이 알림 목록을 조회하면 400을 반환한다.")
  void getNotifications_fail_with_no_limit() throws Exception {
    mockMvc.perform(get("/api/notifications")
            .param("sortDirection", "DESCENDING")
            .param("sortBy", "createdAt"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("알림을 읽음 처리하면 204를 반환한다.")
  void markAsRead_success() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/notifications/" + notificationId))
        .andExpect(status().isNoContent());

    verify(notificationService).markAsRead(eq(notificationId), eq(TEST_RECEIVER_ID));
  }

  @Test
  @DisplayName("존재하지 않는 알림을 읽음 처리하면 404를 반환한다.")
  void markAsRead_fail_with_not_found() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();

    doThrow(new NotificationException(
        NotificationErrorCode.NOTIFICATION_NOT_FOUND,
        Map.of("notificationId", notificationId)
    )).when(notificationService).markAsRead(eq(notificationId), eq(TEST_RECEIVER_ID));

    // when & then
    mockMvc.perform(delete("/api/notifications/" + notificationId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("본인 알림이 아닌 알림을 읽음 처리하면 403을 반환한다.")
  void markAsRead_fail_with_forbidden() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();

    doThrow(new NotificationException(
        NotificationErrorCode.NOTIFICATION_FORBIDDEN,
        Map.of("notificationId", notificationId)
    )).when(notificationService).markAsRead(eq(notificationId), eq(TEST_RECEIVER_ID));

    // when & then
    mockMvc.perform(delete("/api/notifications/" + notificationId))
        .andExpect(status().isForbidden());
  }
}
