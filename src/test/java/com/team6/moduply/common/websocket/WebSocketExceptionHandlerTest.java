package com.team6.moduply.common.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.error.ErrorResponse;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

class WebSocketExceptionHandlerTest {

  private final WebSocketExceptionHandler webSocketExceptionHandler =
      new WebSocketExceptionHandler();

  @Test
  @DisplayName("WebSocket 서비스 예외가 발생하면 공통 에러 응답으로 변환한다.")
  void handleModuPlyException_success_returns_error_response() {
    UUID conversationId = UUID.randomUUID();
    ConversationException exception = new ConversationException(
        ConversationErrorCode.CONVERSATION_NOT_FOUND,
        Map.of("conversationId", conversationId)
    );

    ErrorResponse response = webSocketExceptionHandler.handleModuPlyException(exception);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(response.getCode()).isEqualTo(ConversationErrorCode.CONVERSATION_NOT_FOUND.getCode());
    assertThat(response.getMessage()).isEqualTo(ConversationErrorCode.CONVERSATION_NOT_FOUND.getMessage());
    assertThat(response.getDetails()).containsEntry("conversationId", conversationId);
    assertThat(response.getExceptionType()).isEqualTo(ConversationException.class.getSimpleName());
  }

  @Test
  @DisplayName("WebSocket 권한 예외가 발생하면 403 에러 응답으로 변환한다.")
  void handleAccessDenied_success_returns_forbidden_error_response() {
    AccessDeniedException exception = new AccessDeniedException("인증된 WebSocket 사용자 정보가 없습니다.");

    ErrorResponse response = webSocketExceptionHandler.handleAccessDenied(exception);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(response.getCode()).isEqualTo(HttpStatus.FORBIDDEN.name());
    assertThat(response.getMessage()).isEqualTo("인증된 WebSocket 사용자 정보가 없습니다.");
    assertThat(response.getExceptionType()).isEqualTo(AccessDeniedException.class.getSimpleName());
  }

  @Test
  @DisplayName("WebSocket 일반 예외가 발생하면 500 에러 응답으로 변환한다.")
  void handleException_success_returns_internal_server_error_response() {
    RuntimeException exception = new RuntimeException("unexpected");

    ErrorResponse response = webSocketExceptionHandler.handleException(exception);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(response.getCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.name());
    assertThat(response.getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");
    assertThat(response.getExceptionType()).isEqualTo(RuntimeException.class.getSimpleName());
  }
}
