package com.team6.moduply.follow.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FollowErrorCode implements ErrorCode {
  SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "follow01", "Cannot follow yourself."),
  FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "follow02", "Follow already exists.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
