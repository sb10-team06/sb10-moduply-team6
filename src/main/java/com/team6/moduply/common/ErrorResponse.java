package com.team6.moduply.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

  private final Instant timestamp;
  private final String code;
  private final String message;
  private final Map<String, Object> details;
  private final String exceptionType;
  private final int status;

  // 커스텀 예외 처리용
  public static ErrorResponse from(ModuPlyException ex) {
    return new ErrorResponse(
        Instant.now(),
        ex.getErrorCode().getCode(),
        ex.getMessage(),
        ex.getDetails(),
        ex.getClass().getSimpleName(),
        ex.getErrorCode().getHttpStatus().value()
    );
  }

  // @Valid 검증 실패 처리용 (필드별 에러 details 포함)
  public static ErrorResponse from(HttpStatus status, Map<String, Object> details, Exception ex) {
    return new ErrorResponse(
        Instant.now(),
        status.name(),
        "입력값 검증에 실패하였습니다.",
        details,
        ex.getClass().getSimpleName(),
        status.value()
    );
  }

  // 일반 예외 처리용 (내부 정보 노출 방지)
  public static ErrorResponse from(HttpStatus status, Exception ex) {
    return new ErrorResponse(
        Instant.now(),
        status.name(),
        status.is5xxServerError() ? "서버 내부 오류가 발생했습니다." : ex.getMessage(),
        new HashMap<>(),
        ex.getClass().getSimpleName(),
        status.value()
    );
  }
}
