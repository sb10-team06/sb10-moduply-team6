package com.team6.moduply.conversation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.service.ConversationService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ConversationController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
class ConversationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ConversationService conversationService;

  @Test
  @DisplayName("새 대화방을 생성하면 201 응답을 반환한다.")
  void createConversation_success_when_created() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    ConversationDto response = conversationDto(conversationId, withUserId);

    given(conversationService.create(eq(request), eq(currentUserId))).willReturn(response);

    mockMvc.perform(post("/api/conversations")
            .with(user(userDetails(currentUserId)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(conversationId.toString()))
        .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
        .andExpect(jsonPath("$.with.name").value("with"))
        .andExpect(jsonPath("$.with.profileImageUrl").value("https://example.com/profile.png"))
        .andExpect(jsonPath("$.lastestMessage").doesNotExist())
        .andExpect(jsonPath("$.hasUnread").value(false));

    verify(conversationService).create(request, currentUserId);
  }

  @Test
  @DisplayName("대화 상대 사용자 ID가 누락되면 400 응답을 반환한다.")
  void createConversation_fail_when_with_user_id_is_missing() throws Exception {
    UUID currentUserId = UUID.randomUUID();

    mockMvc.perform(post("/api/conversations")
            .with(user(userDetails(currentUserId)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());

    verify(conversationService, never()).create(any(), eq(currentUserId));
  }

  @Test
  @DisplayName("대화방 단건 조회 요청 시 인증 사용자 ID를 서비스에 전달하고 200 응답을 반환한다.")
  void findConversationById_success_with_authenticated_principal() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    ConversationDto response = conversationDto(conversationId, withUserId);

    given(conversationService.findById(eq(conversationId), eq(currentUserId))).willReturn(response);

    mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(conversationId.toString()))
        .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
        .andExpect(jsonPath("$.hasUnread").value(false));

    verify(conversationService).findById(conversationId, currentUserId);
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 요청 시 인증 사용자 ID와 대상 사용자 ID를 서비스에 전달하고 200 응답을 반환한다.")
  void findConversationWithUser_success_with_authenticated_principal() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    ConversationDto response = conversationDto(conversationId, withUserId);

    given(conversationService.findByUserId(eq(withUserId), eq(currentUserId))).willReturn(response);

    mockMvc.perform(get("/api/conversations/with")
            .queryParam("userId", withUserId.toString())
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(conversationId.toString()))
        .andExpect(jsonPath("$.with.userId").value(withUserId.toString()))
        .andExpect(jsonPath("$.hasUnread").value(false));

    verify(conversationService).findByUserId(withUserId, currentUserId);
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 요청 시 사용자 ID가 없으면 400 응답을 반환한다.")
  void findConversationWithUser_fail_when_user_id_is_missing() throws Exception {
    UUID currentUserId = UUID.randomUUID();

    mockMvc.perform(get("/api/conversations/with")
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isBadRequest());

    verify(conversationService, never()).findByUserId(any(), eq(currentUserId));
  }

  private ConversationDto conversationDto(UUID conversationId, UUID withUserId) {
    UserSummaryDto withUser = new UserSummaryDto(
        withUserId,
        "with",
        "https://example.com/profile.png"
    );
    return new ConversationDto(conversationId, withUser, null, false);
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
    return new ModuPlyUserDetails(userDto, "password");
  }
}
