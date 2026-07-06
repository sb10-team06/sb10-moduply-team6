package com.team6.moduply.directmessage.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.directmessage.dto.DirectMessageCreateRequest;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.service.DirectMessageService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DirectMessageWebSocketControllerTest {

  @InjectMocks
  private DirectMessageWebSocketController directMessageWebSocketController;

  @Mock
  private DirectMessageService directMessageService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Test
  @DisplayName("WebSocket DM 생성 요청을 받으면 메시지를 저장하고 대화방 구독 경로로 전송한다.")
  void create_success_broadcasts_saved_direct_message() {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");
    DirectMessageDto response = new DirectMessageDto(
        directMessageId,
        conversationId,
        Instant.parse("2026-01-01T00:00:00Z"),
        null,
        null,
        "hello"
    );
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(
        userDetails(currentUserId),
        null
    );

    given(directMessageService.create(conversationId, request, currentUserId))
        .willReturn(response);

    directMessageWebSocketController.create(conversationId, request, authentication);

    verify(directMessageService).create(conversationId, request, currentUserId);
    verify(messagingTemplate).convertAndSend(
        "/sub/conversations/" + conversationId + "/direct-messages",
        response
    );
  }

  @Test
  @DisplayName("인증된 WebSocket 사용자 정보가 없으면 DM을 생성하지 않고 예외가 발생한다.")
  void create_fail_when_principal_is_invalid() {
    UUID conversationId = UUID.randomUUID();
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");

    assertThatThrownBy(() -> directMessageWebSocketController.create(conversationId, request, null))
        .isInstanceOf(AccessDeniedException.class);

    verify(directMessageService, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
    verify(messagingTemplate, never()).convertAndSend(
        org.mockito.ArgumentMatchers.any(String.class),
        org.mockito.ArgumentMatchers.any(Object.class)
    );
  }

  @Test
  @DisplayName("DM 생성 중 예외가 발생하면 대화방 구독 경로로 전송하지 않는다.")
  void create_fail_when_service_throws_exception() {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(
        userDetails(currentUserId),
        null
    );

    given(directMessageService.create(conversationId, request, currentUserId))
        .willThrow(new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));

    assertThatThrownBy(() -> directMessageWebSocketController.create(
        conversationId,
        request,
        authentication
    ))
        .isInstanceOf(ConversationException.class);

    verify(messagingTemplate, never()).convertAndSend(
        org.mockito.ArgumentMatchers.any(String.class),
        org.mockito.ArgumentMatchers.any(Object.class)
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
    return new ModuPlyUserDetails(userDto, "password");
  }
}
