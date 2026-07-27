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
import org.springframework.security.authentication.LockedException;
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

    logAuthenticationFailure(request, exception, authException);

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

    if (authException instanceof LockedException || cause instanceof LockedException) {
      return new AuthException(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION, Collections.emptyMap());
    }

    if (authException instanceof BadCredentialsException) {
      return new AuthException(AuthErrorCode.INVALID_TOKEN_EXCEPTION, Collections.emptyMap());
    }

    return new AuthException(AuthErrorCode.MISSING_TOKEN_EXCEPTION, Map.of(
        "reason", "EntryPoint01"
    ));
  }

  private void logAuthenticationFailure(HttpServletRequest request, AuthException exception,
      AuthenticationException authException) {
    String uri = request.getRequestURI();
    String code = exception.getErrorCode().getCode();
    String exceptionName = authException.getClass().getSimpleName();

    if (isServicePath(uri)) {
      log.warn("인증 실패: uri={}, code={}, exception={}", uri, code, exceptionName);
      return;
    }

    log.debug("비서비스 경로 인증 실패: uri={}, code={}, exception={}", uri, code, exceptionName);
  }

  private boolean isServicePath(String uri) {
    return uri.equals("/")
        || uri.equals("/index.html")
        || uri.equals("/favicon.svg")
        || uri.equals("/placeholder-movie.png")
        || uri.startsWith("/api/")
        || uri.startsWith("/ws")
        || uri.startsWith("/uploads/")
        || uri.startsWith("/assets/")
        || uri.startsWith("/oauth2/")
        || uri.startsWith("/login/oauth2/");
  }
}
