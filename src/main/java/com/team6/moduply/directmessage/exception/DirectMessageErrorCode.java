package com.team6.moduply.directmessage.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DirectMessageErrorCode implements ErrorCode {
  DIRECT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "directMessage01", "다이렉트 메시지 정보를 찾을 수 없습니다."),
  DIRECT_MESSAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "directMessage02", "해당 다이렉트 메시지에 대한 권한이 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
