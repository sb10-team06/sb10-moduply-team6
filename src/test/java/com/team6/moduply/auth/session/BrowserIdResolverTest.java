package com.team6.moduply.auth.session;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class BrowserIdResolverTest {

  private final BrowserIdResolver browserIdResolver = new BrowserIdResolver();

  @Test
  @DisplayName("Browser ID 쿠키가 없으면 SameSite=Lax 보안 속성을 포함해 새로 발급한다")
  void resolve_or_create_issues_browser_id_cookie_with_same_site_lax() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    String browserId = browserIdResolver.resolveOrCreate(request, response);

    // Then
    Cookie browserIdCookie = response.getCookie(BrowserIdResolver.BROWSER_ID_COOKIE_NAME);

    assertThat(UUID.fromString(browserId)).isNotNull();
    assertThat(browserIdCookie).isNotNull();
    assertThat(browserIdCookie.getValue()).isEqualTo(browserId);
    assertThat(browserIdCookie.isHttpOnly()).isTrue();
    assertThat(browserIdCookie.getSecure()).isTrue();
    assertThat(browserIdCookie.getPath()).isEqualTo("/");
    assertThat(browserIdCookie.getAttribute("SameSite")).isEqualTo("Lax");
  }

  @Test
  @DisplayName("유효한 Browser ID 쿠키가 있으면 새 쿠키를 발급하지 않고 기존 값을 사용한다")
  void resolve_or_create_reuses_existing_browser_id_cookie() {
    // Given
    String browserId = UUID.randomUUID().toString();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(BrowserIdResolver.BROWSER_ID_COOKIE_NAME, browserId));
    MockHttpServletResponse response = new MockHttpServletResponse();

    // When
    String resolvedBrowserId = browserIdResolver.resolveOrCreate(request, response);

    // Then
    assertThat(resolvedBrowserId).isEqualTo(browserId);
    assertThat(response.getCookie(BrowserIdResolver.BROWSER_ID_COOKIE_NAME)).isNull();
  }
}
