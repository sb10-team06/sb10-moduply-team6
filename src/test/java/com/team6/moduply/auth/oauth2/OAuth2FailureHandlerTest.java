package com.team6.moduply.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.oauth2.handler.OAuth2FailureHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2FailureHandlerTest {

  @Test
  @DisplayName("OAuth2 잠금 계정 예외는 로그인 화면에 잠금 에러 코드로 리다이렉트한다")
  void oauth2_locked_account_failure_redirects_to_sign_in_with_error_code() throws Exception {
    // Given
    OAuth2FailureHandler handler = new OAuth2FailureHandler();
    ReflectionTestUtils.setField(handler, "redirectUrl", "http://localhost:8080/#/sign-in");

    OAuth2Error oauth2Error = new OAuth2Error(
        AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION.getCode(),
        AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION.getMessage(),
        null
    );
    OAuth2AuthenticationException exception = new OAuth2AuthenticationException(oauth2Error);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:8080/#/sign-in?error=auth03");
  }

  @Test
  @DisplayName("OAuth2 외 인증 예외는 기본 로그인 실패 에러 코드로 리다이렉트한다")
  void non_oauth2_failure_redirects_to_sign_in_with_bad_credentials_code() throws Exception {
    // Given
    OAuth2FailureHandler handler = new OAuth2FailureHandler();
    ReflectionTestUtils.setField(handler, "redirectUrl", "http://localhost:8080/#/sign-in");

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad credentials"));

    // Then
    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:8080/#/sign-in?error=auth02");
  }
}
