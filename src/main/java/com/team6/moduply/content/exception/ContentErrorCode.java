package com.team6.moduply.content.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContentErrorCode implements ErrorCode {
  CONTENT_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "content01", "콘텐츠 생성 권한이 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
