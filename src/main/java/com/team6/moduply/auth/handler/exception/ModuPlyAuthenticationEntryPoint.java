package com.team6.moduply.auth.handler.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ModuPlyAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    AuthException exception = resolveAuthException(authException);

    log.warn("인증 실패: uri={}, code={}, exception={}",
        request.getRequestURI(),
        exception.getErrorCode().getCode(),
        authException.getClass().getSimpleName()
    );

    response.setStatus(exception.getErrorCode().getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(response.getWriter(), ErrorResponse.from(exception));
  }

  private AuthException resolveAuthException(AuthenticationException authException) {
    Throwable cause = authException.getCause();
    if (cause instanceof AuthException exception) {
      return exception;
    }

    if (authException instanceof BadCredentialsException) {
      return new AuthException(AuthErrorCode.INVALID_TOKEN_EXCEPTION, Collections.emptyMap());
    }

    return new AuthException(AuthErrorCode.MISSING_TOKEN_EXCEPTION, Map.of(
        "reason", "EntryPoint01"
    ));
  }
}
