package com.team6.moduply.common.websocket;

import com.team6.moduply.common.error.ErrorResponse;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;

/// 전역 WebSocket 예외 핸들러
/// @MessageMaping 처리중 터진 서비스, 권한, payload 예외처리
@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

  private static final String USER_ERROR_DESTINATION = "/queue/errors";

  @MessageExceptionHandler(ModuPlyException.class)
  @SendToUser(value = USER_ERROR_DESTINATION, broadcast = false)
  public ErrorResponse handleModuPlyException(ModuPlyException ex) {
    HttpStatus status = ex.getErrorCode().getHttpStatus();
    if (status.is5xxServerError()) {
      log.error("[WebSocket:{}] {} - details: {}", ex.getClass().getSimpleName(), ex.getMessage(),
          ex.getDetails(), ex);
    } else {
      log.warn("[WebSocket:{}] {} - details: {}", ex.getClass().getSimpleName(), ex.getMessage(),
          ex.getDetails());
    }
    return ErrorResponse.from(ex);
  }

  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  @SendToUser(value = USER_ERROR_DESTINATION, broadcast = false)
  public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.groupingBy(
            FieldError::getField,
            Collectors.mapping(
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage()
                    : "유효하지 않은 값입니다",
                Collectors.toList()
            )
        ));
    Map<String, Object> details = new HashMap<>(fieldErrors);
    log.warn("[WebSocket:MethodArgumentNotValidException] {}", ex.getMessage());
    return ErrorResponse.from(HttpStatus.BAD_REQUEST, details, ex);
  }

  @MessageExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
  @SendToUser(value = USER_ERROR_DESTINATION, broadcast = false)
  public ErrorResponse handleAccessDenied(Exception ex) {
    log.warn("[WebSocket:{}] {}", ex.getClass().getSimpleName(), ex.getMessage());
    return ErrorResponse.from(HttpStatus.FORBIDDEN, ex);
  }

  @MessageExceptionHandler(IllegalArgumentException.class)
  @SendToUser(value = USER_ERROR_DESTINATION, broadcast = false)
  public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("[WebSocket:IllegalArgumentException] {}", ex.getMessage());
    return ErrorResponse.from(HttpStatus.BAD_REQUEST, ex);
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser(value = USER_ERROR_DESTINATION, broadcast = false)
  public ErrorResponse handleException(Exception ex) {
    log.error("[WebSocket:Exception] 예상치 못한 오류 발생", ex);
    return ErrorResponse.from(HttpStatus.INTERNAL_SERVER_ERROR, ex);
  }
}
