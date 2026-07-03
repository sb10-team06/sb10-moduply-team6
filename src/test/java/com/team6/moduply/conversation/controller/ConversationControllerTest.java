package com.team6.moduply.conversation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.dto.ConversationSortBy;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.service.ConversationService;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.dto.DirectMessageSortBy;
import com.team6.moduply.directmessage.exception.DirectMessageErrorCode;
import com.team6.moduply.directmessage.exception.DirectMessageException;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.enums.Role;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
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
  @DisplayName("존재하지 않는 대화방을 조회하면 404 응답을 반환한다.")
  void findConversationById_fail_when_conversation_not_found() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();

    given(conversationService.findById(eq(conversationId), eq(currentUserId)))
        .willThrow(new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));

    mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ConversationErrorCode.CONVERSATION_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.message").value(
            ConversationErrorCode.CONVERSATION_NOT_FOUND.getMessage()));

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
  @DisplayName("대화 목록 조회 요청 시 인증 사용자 ID를 서비스에 전달하고 200 응답을 반환한다.")
  void findConversations_success_with_authenticated_principal() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    ConversationDto conversation = conversationDto(conversationId, withUserId);
    CursorResponse<ConversationDto> response = new CursorResponse<>(
        List.of(conversation),
        null,
        null,
        false,
        1L,
        ConversationSortBy.createdAt.name(),
        SortDirection.DESCENDING
    );

    given(conversationService.findAll(any(), eq(currentUserId))).willReturn(response);

    mockMvc.perform(get("/api/conversations")
            .queryParam("limit", "10")
            .queryParam("sortDirection", "DESCENDING")
            .queryParam("sortBy", "createdAt")
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(conversationId.toString()))
        .andExpect(jsonPath("$.data[0].with.userId").value(withUserId.toString()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.sortBy").value("createdAt"))
        .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));

    verify(conversationService).findAll(any(), eq(currentUserId));
  }

  @Test
  @DisplayName("대화 목록 조회 요청 시 필수 쿼리 파라미터가 없으면 400 응답을 반환한다.")
  void findConversations_fail_when_required_query_parameters_are_missing() throws Exception {
    UUID currentUserId = UUID.randomUUID();

    mockMvc.perform(get("/api/conversations")
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isBadRequest());

    verify(conversationService, never()).findAll(any(), eq(currentUserId));
  }

  @Test
  @DisplayName("대화 목록 조회 요청의 cursor가 파싱할 수 없는 날짜이면 400 응답을 반환한다.")
  void findConversations_fail_when_cursor_is_not_parseable() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID idAfter = UUID.randomUUID();

    mockMvc.perform(get("/api/conversations")
            .queryParam("cursor", "2024-13-40T99:99:99Z")
            .queryParam("idAfter", idAfter.toString())
            .queryParam("limit", "10")
            .queryParam("sortDirection", "DESCENDING")
            .queryParam("sortBy", "createdAt")
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isBadRequest());

    verify(conversationService, never()).findAll(any(), eq(currentUserId));
  }

  @Test
  @DisplayName("DM 목록 조회 요청 시 인증 사용자 ID를 서비스에 전달하고 200 응답을 반환한다.")
  void findDms_success_with_authenticated_principal() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    DirectMessageDto directMessage = new DirectMessageDto(
        directMessageId,
        conversationId,
        createdAt,
        null,
        null,
        "hello"
    );
    CursorResponse<DirectMessageDto> response = new CursorResponse<>(
        List.of(directMessage),
        null,
        null,
        false,
        1L,
        DirectMessageSortBy.createdAt.name(),
        SortDirection.DESCENDING
    );

    given(conversationService.findDms(eq(conversationId), any(), eq(currentUserId)))
        .willReturn(response);

    mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
            .queryParam("limit", "10")
            .queryParam("sortDirection", "DESCENDING")
            .queryParam("sortBy", "createdAt")
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(directMessageId.toString()))
        .andExpect(jsonPath("$.data[0].conversationId").value(conversationId.toString()))
        .andExpect(jsonPath("$.data[0].content").value("hello"))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.sortBy").value("createdAt"))
        .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));

    verify(conversationService).findDms(eq(conversationId), any(), eq(currentUserId));
  }

  @Test
  @DisplayName("DM 목록 조회 요청 시 필수 쿼리 파라미터가 없으면 400 응답을 반환한다.")
  void findDms_fail_when_required_query_parameters_are_missing() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();

    mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isBadRequest());

    verify(conversationService, never()).findDms(eq(conversationId), any(), eq(currentUserId));
  }

  @Test
  @DisplayName("DM 목록 조회 요청의 cursor가 파싱할 수 없는 날짜이면 400 응답을 반환한다.")
  void findDms_fail_when_cursor_is_not_parseable() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID idAfter = UUID.randomUUID();

    mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
            .queryParam("cursor", "2024-13-40T99:99:99Z")
            .queryParam("idAfter", idAfter.toString())
            .queryParam("limit", "10")
            .queryParam("sortDirection", "DESCENDING")
            .queryParam("sortBy", "createdAt")
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isBadRequest());

    verify(conversationService, never()).findDms(eq(conversationId), any(), eq(currentUserId));
  }

  @Test
  @DisplayName("DM 목록 조회 요청 시 대화방 참여자가 아니면 403 응답을 반환한다.")
  void findDms_fail_when_user_is_not_participant() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();

    given(conversationService.findDms(eq(conversationId), any(), eq(currentUserId)))
        .willThrow(new ConversationException(
            ConversationErrorCode.CONVERSATION_FORBIDDEN,
            Map.of("conversationId", conversationId, "userId", currentUserId)
        ));

    mockMvc.perform(get("/api/conversations/{conversationId}/direct-messages", conversationId)
            .queryParam("limit", "10")
            .queryParam("sortDirection", "DESCENDING")
            .queryParam("sortBy", "createdAt")
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ConversationErrorCode.CONVERSATION_FORBIDDEN.getCode()));

    verify(conversationService).findDms(eq(conversationId), any(), eq(currentUserId));
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

  @Test
  @DisplayName("특정 사용자와의 대화 조회 요청 시 자기 자신을 조회하면 400 응답을 반환한다.")
  void findConversationWithUser_fail_when_user_is_self() throws Exception {
    UUID currentUserId = UUID.randomUUID();

    given(conversationService.findByUserId(eq(currentUserId), eq(currentUserId)))
        .willThrow(new ConversationException(
            ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED,
            Map.of("userId", currentUserId)
        ));

    mockMvc.perform(get("/api/conversations/with")
            .queryParam("userId", currentUserId.toString())
            .with(user(userDetails(currentUserId))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED.getCode()));

    verify(conversationService).findByUserId(currentUserId, currentUserId);
  }

  @Test
  @DisplayName("DM 읽음 처리 요청 시 인증 사용자 ID를 서비스에 전달하고 200 응답을 반환한다.")
  void read_success_with_authenticated_principal() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();

    mockMvc.perform(post("/api/conversations/{conversationId}/direct-messages/{directMessageId}/read",
            conversationId,
            directMessageId)
            .with(user(userDetails(currentUserId)))
            .with(csrf()))
        .andExpect(status().isOk());

    verify(conversationService).read(conversationId, directMessageId, currentUserId);
  }

  @Test
  @DisplayName("DM 발신자가 본인 메시지 읽음 처리를 요청하면 403 응답을 반환한다.")
  void read_fail_when_sender_requests() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();

    willThrow(new DirectMessageException(
            DirectMessageErrorCode.DIRECT_MESSAGE_FORBIDDEN,
            Map.of("directMessageId", directMessageId, "userId", currentUserId)
        ))
        .given(conversationService)
        .read(eq(conversationId), eq(directMessageId), eq(currentUserId));

    mockMvc.perform(post("/api/conversations/{conversationId}/direct-messages/{directMessageId}/read",
            conversationId,
            directMessageId)
            .with(user(userDetails(currentUserId)))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(DirectMessageErrorCode.DIRECT_MESSAGE_FORBIDDEN.getCode()));

    verify(conversationService).read(conversationId, directMessageId, currentUserId);
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
