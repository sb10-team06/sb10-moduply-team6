package com.team6.moduply.auth.handler.login;

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
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ModuPlyLoginFailureHandler implements AuthenticationFailureHandler {
  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authenticationException) throws IOException {
    AuthException exception = resolveAuthException(authenticationException);

    log.warn("로그인 실패: uri={}, code={}, exception={}",
        request.getRequestURI(),
        exception.getErrorCode().getCode(),
        authenticationException.getClass().getSimpleName()
    );

    response.setStatus(exception.getErrorCode().getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(response.getWriter(), ErrorResponse.from(exception));
  }

  private AuthException resolveAuthException(AuthenticationException authenticationException) {
    if (authenticationException instanceof BadCredentialsException) {
      return new AuthException(AuthErrorCode.BAD_CREDENTIALS_EXCEPTION, Collections.emptyMap());
    }

    if (authenticationException instanceof UsernameNotFoundException
        || authenticationException instanceof InternalAuthenticationServiceException) {
      return new AuthException(AuthErrorCode.USERNAME_NOT_FOUND_EXCEPTION, Collections.emptyMap());
    }

    if (authenticationException instanceof LockedException) {
      return new AuthException(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION, Collections.emptyMap());
    }

    return new AuthException(AuthErrorCode.BAD_CREDENTIALS_EXCEPTION, Map.of(
        "reason", authenticationException.getClass().getSimpleName()
    ));
  }
}
