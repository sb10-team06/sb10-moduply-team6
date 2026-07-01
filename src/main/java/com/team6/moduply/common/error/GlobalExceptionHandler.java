package com.team6.moduply.common.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // ModuPlyException: ErrorCode에 정의된 모든 커스텀 예외 통합 처리
  // 5xx면 error, 나머지는 warn으로 로그 레벨 구분
  @ExceptionHandler(ModuPlyException.class)
  public ResponseEntity<ErrorResponse> handleModuPlyException(ModuPlyException ex) {
    HttpStatus status = ex.getErrorCode().getHttpStatus();
    if (status.is5xxServerError()) {
      log.error("[{}] {} - details: {}", ex.getClass().getSimpleName(), ex.getMessage(),
          ex.getDetails(), ex);
    } else {
      log.warn("[{}] {} - details: {}", ex.getClass().getSimpleName(), ex.getMessage(),
          ex.getDetails());
    }
    return ResponseEntity.status(status).body(ErrorResponse.from(ex));
  }

  // @Valid 검증 실패 시 발생
  // BindingResult에서 필드별 에러 메시지를 Map으로 변환하여 details에 담아 반환
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    // 동일 필드에 여러 검증 에러 발생 시 List로 수집 (중복 키 IllegalStateException 방지)
    Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.groupingBy(
            FieldError::getField,
            Collectors.mapping(
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage()
                    : "유효하지 않은 값입니다",
                Collectors.toList()
            )
        ));
    Map<String, Object> details = new java.util.HashMap<>(fieldErrors);
    log.warn("[MethodArgumentNotValidException] {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.from(HttpStatus.BAD_REQUEST, details, ex));
  }

  /*
  Monew-Request-User-Id 헤더 -> 인증 목적으로 사용하기에 UNAUTHORIZED가 맞다고 판단
  - 로그인 이후 Monew-Request-User-ID 헤더로 로그인한 유저 식별
  - 로그인/회원가입은 인증 전 단계라 헤더 없음
  - 그 외 모든 엔드포인트는 @RequestHeader 선언 → 헤더 누락 시 MissingRequestHeaderException 발생 → 401 Unauthorized 반환
  - 현재는 단일 헤더만 사용하는 구조라 위 방식으로 처리하지만, 추후 Spring Security로 인증 로직 이관 예정 (강사님 말씀)
   */
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
    log.warn("[MissingRequestHeaderException] {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.from(HttpStatus.UNAUTHORIZED, ex));
  }

  // 클라이언트 요청 오류
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("[IllegalArgumentException] {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.from(HttpStatus.BAD_REQUEST, ex));
  }

  /// 입력 포멧 오류: 400으로 처리
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    log.warn("[MethodArgumentTypeMismatchException] {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.from(HttpStatus.BAD_REQUEST, ex));
  }

  /// @RequestParam 누란된경우
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
      MissingServletRequestParameterException ex
  ) {
    log.warn("[MissingServletRequestParameterException] {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.from(HttpStatus.BAD_REQUEST, ex));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
    log.warn("[HttpMediaTypeNotSupportedException] {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(ErrorResponse.from(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex));
  }

  // @PreAuthorize 등 메서드 시큐리티 인가 실패
  @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
  public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex) {
    log.warn("[{}] {}", ex.getClass().getSimpleName(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.from(HttpStatus.FORBIDDEN, ex));
  }

  // 클라이언트가 응답 완료 전에 연결을 끊은 경우
  // 브라우저 새로고침, 요청 취소, Prometheus scrape 중단 등으로 발생할 수 있으므로 서버 오류로 기록하지 않는다.
  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public ResponseEntity<Void> handleAsyncRequestNotUsable(
      AsyncRequestNotUsableException ex,
      HttpServletRequest request
  ) {
    log.debug("[AsyncRequestNotUsableException] client aborted request. uri={}",
        request.getRequestURI());
    return ResponseEntity.noContent().build();
  }

  // 위에서 처리되지 않은 모든 예외 처리 (예상치 못한 서버 오류)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
    String uri = request.getRequestURI();

    // actuator/prometheus는 application/openmetrics-text 응답을 사용한다.
    // 여기서 ErrorResponse(JSON)를 반환하면 Content-Type 충돌이 발생하므로 공통 예외 응답을 적용하지 않는다.
    if (uri.startsWith("/actuator/prometheus")) {
      log.warn("[Exception] actuator prometheus scrape failed. uri={}", uri, ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


    log.error("[Exception] 예상치 못한 오류 발생 ", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.from(HttpStatus.INTERNAL_SERVER_ERROR, ex));
  }
}
