package com.team6.moduply.content.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContentErrorCode implements ErrorCode {
  CONTENT_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "content01", "콘텐츠 생성 권한이 없습니다."),
  CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "content02", "콘텐츠를 찾을 수 없습니다."),
  INVALID_CURSOR(HttpStatus.BAD_REQUEST, "content03", "커서 정보가 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
