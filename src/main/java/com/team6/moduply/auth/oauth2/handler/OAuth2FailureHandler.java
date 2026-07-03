package com.team6.moduply.auth.oauth2.handler;

import com.team6.moduply.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Value("${jwt.oauth-failure-url}")
  private String redirectUrl;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {
    String errorCode = exception instanceof OAuth2AuthenticationException oauthException
        ? oauthException.getError().getErrorCode()
        : AuthErrorCode.BAD_CREDENTIALS_EXCEPTION.getCode();

    log.warn("OAuth2 로그인 실패: uri={}, code={}, message={}",
        request.getRequestURI(), errorCode, exception.getMessage());

    // 만약 리다이랙션 URI에 이미 다른 파라미터가 있을걸 대비
    String separator = redirectUrl.contains("?") ? "&" : "?";
    String targetUrl = redirectUrl + separator + "error="
        + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
