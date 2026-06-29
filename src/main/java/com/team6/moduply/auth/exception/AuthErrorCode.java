package com.team6.moduply.auth.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  USERNAME_NOT_FOUND_EXCEPTION(HttpStatus.UNAUTHORIZED, "auth01", "이메일 또는 비밀번호가 일치하지 않습니다."),
  BAD_CREDENTIALS_EXCEPTION(HttpStatus.UNAUTHORIZED, "auth02", "이메일 또는 비밀번호가 일치하지 않습니다."),
  ACCOUNT_LOCKED_EXCEPTION(HttpStatus.FORBIDDEN, "auth03", "잠긴 계정입니다."),
  ACCESS_DENIED_EXCEPTION(HttpStatus.FORBIDDEN, "auth04", "접근 권한이 없습니다."),
  TOKEN_GENERATION_FAILED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "auth05", "토큰 생성에 실패했습니다."),
  INVALID_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "auth06", "유효하지 않은 토큰입니다."),
  EXPIRED_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "auth07", "만료된 토큰입니다."),
  UNSUPPORTED_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "auth08", "지원하지 않는 토큰입니다."),
  MISSING_TOKEN_EXCEPTION(HttpStatus.UNAUTHORIZED, "auth09", "인증 토큰이 없습니다."),
  INVALID_JWT_SECRET_KEY_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "auth10",
      "JWT 비밀키는 HS256 서명에 필요한 최소 길이를 충족해야 합니다."),
  INVALID_TEMP_PASSWORD_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "auth11",
      "임시 비밀번호의 길이는 최소 8자리 이상이어야 합니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
