package com.team6.moduply.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class SpaCsrfTokenRequestHandlerTest {

  private final SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();

  @Test
  @DisplayName("CSRF 토큰을 지연 상태로 두지 않고 resolve한다.")
  void handle_resolves_deferred_csrf_token() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "csrf-token");
    AtomicInteger resolveCount = new AtomicInteger();

    // When
    handler.handle(request, response, () -> {
      resolveCount.incrementAndGet();
      return csrfToken;
    });

    // Then
    assertThat(resolveCount).hasValue(1);
  }

  @Test
  @DisplayName("CSRF 헤더가 있으면 SPA가 보낸 평문 토큰을 사용한다.")
  void resolve_csrf_token_value_uses_plain_header_token() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "csrf-token");
    request.addHeader(csrfToken.getHeaderName(), csrfToken.getToken());

    // When
    String resolvedToken = handler.resolveCsrfTokenValue(request, csrfToken);

    // Then
    assertThat(resolvedToken).isEqualTo(csrfToken.getToken());
  }
}
