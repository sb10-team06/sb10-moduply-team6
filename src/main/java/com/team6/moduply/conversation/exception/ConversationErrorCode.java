package com.team6.moduply.conversation.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ConversationErrorCode implements ErrorCode {
  SELF_CONVERSATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "conversation01", "자기 자신과 대화방을 생성할 수 없습니다."),
  CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "conversation02", "대화방 정보를 찾을 수 없습니다."),
  CONVERSATION_FORBIDDEN(HttpStatus.FORBIDDEN, "conversation03", "해당 대화방에 대한 권한이 없습니다."),
  CONVERSATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "conversation04", "이미 존재하는 대화방입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
