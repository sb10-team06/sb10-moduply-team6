package com.team6.moduply.watching.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatchingSessionErrorCode implements ErrorCode {

  WATCHING_SESSION__NOT_FOUND(HttpStatus.NOT_FOUND, "watching-session01", "시청세션을 찾을 수 없습니다."),
  WATCHING_SESSION__CONTENT_MISMATCH(HttpStatus.BAD_REQUEST, "watching-session02",
      "시청세션과 컨텐츠 채팅 세션이 불일치합니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
