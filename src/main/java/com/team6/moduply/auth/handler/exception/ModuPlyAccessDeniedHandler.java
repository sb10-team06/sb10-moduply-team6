package com.team6.moduply.auth.handler.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ModuPlyAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    AuthException exception = new AuthException(
        AuthErrorCode.ACCESS_DENIED_EXCEPTION,
        Collections.emptyMap()
    );

    log.warn("인가 실패: uri={}, code={}, exception={}",
        request.getRequestURI(),
        exception.getErrorCode().getCode(),
        accessDeniedException.getClass().getSimpleName()
    );

    response.setStatus(exception.getErrorCode().getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(response.getWriter(), ErrorResponse.from(exception));
  }
}
