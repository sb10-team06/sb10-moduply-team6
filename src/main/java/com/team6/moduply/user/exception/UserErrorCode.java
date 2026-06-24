package com.team6.moduply.user.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
  DUPLICATED_EMAIL_EXCEPTION(HttpStatus.CONFLICT, "DUPLICATED_EMAIL_EXCEPTION", "S3 파일 업로드에 실패했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
