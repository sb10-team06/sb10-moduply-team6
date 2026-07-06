package com.team6.moduply.notification.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "notification01", "알림을 찾을 수 없습니다."),
  NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "notification02", "알림에 대한 권한이 없습니다."),
  NOTIFICATION_INVALID_STATE(HttpStatus.INTERNAL_SERVER_ERROR, "notification03", "알림 데이터가 유효하지 않습니다."),
  NOTIFICATION_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "notification04", "커서 형식이 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
