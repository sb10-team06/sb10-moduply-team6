package com.team6.moduply.auth.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BrowserIdResolver {

  public static final String BROWSER_ID_COOKIE_NAME = "BROWSER_ID";
  private static final String SAME_SITE_ATTRIBUTE = "SameSite";
  private static final String SAME_SITE_LAX = "Lax";
  private static final int BROWSER_ID_MAX_AGE_SECONDS = 60 * 60 * 24 * 90;

  /**
   * 브라우저를 구분하기 위한 서버 발급 식별자를 쿠키에서 읽거나 새로 발급한다.
   *
   * <p>중복 세션 방지는 userId만으로 판단하면 다른 브라우저 로그인을 막아버린다.
   * 그래서 userId + browserId 조합을 사용한다. browserId는 보안 인증 수단이 아니라
   * "같은 브라우저인지"를 구분하기 위한 힌트다.</p>
   *
   * <p>쿠키를 삭제하거나 시크릿 모드를 쓰면 새 브라우저로 취급된다. 이는 브라우저를
   * 완벽히 식별할 수 없기 때문에 감수하는 정책적 한계다.</p>
   */
  public String resolveOrCreate(HttpServletRequest request, HttpServletResponse response) {
    String browserId = resolve(request);
    if (browserId != null) {
      return browserId;
    }

    String newBrowserId = UUID.randomUUID().toString();
    Cookie cookie = new Cookie(BROWSER_ID_COOKIE_NAME, newBrowserId);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(BROWSER_ID_MAX_AGE_SECONDS);
    cookie.setAttribute(SAME_SITE_ATTRIBUTE, SAME_SITE_LAX);
    response.addCookie(cookie);
    return newBrowserId;
  }

  private String resolve(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (BROWSER_ID_COOKIE_NAME.equals(cookie.getName()) && isValidUuid(cookie.getValue())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private boolean isValidUuid(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
