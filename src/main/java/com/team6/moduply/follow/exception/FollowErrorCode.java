package com.team6.moduply.follow.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FollowErrorCode implements ErrorCode {
  SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "follow01", "자기자신은 팔로우 할 수 없습니다."),
  FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "follow02", "이미 팔로우한 사용자입니다."),
  FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "follow03", "팔로우 정보를 찾을 수 없습니다."),
  FOLLOW_FORBIDDEN(HttpStatus.FORBIDDEN, "follow04", "해당 팔로우에 대한 권한이 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
