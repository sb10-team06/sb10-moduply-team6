package com.team6.moduply.user.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
  DUPLICATED_EMAIL_EXCEPTION(HttpStatus.CONFLICT, "user01", "중복된 이메일입니다."),
  USER_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "user02", "사용자를 찾을 수 없습니다."),
  PROFILE_IMAGE_UPLOAD_FAILED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "user03", "프로필 이미지 업로드에 실패했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
