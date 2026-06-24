package com.team6.moduply.user.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
  DUPLICATED_EMAIL_EXCEPTION(HttpStatus.CONFLICT, "DUPLICATED_EMAIL_EXCEPTION", "중복된 이메일입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
