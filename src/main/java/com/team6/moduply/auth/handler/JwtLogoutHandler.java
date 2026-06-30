package com.team6.moduply.auth.handler;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisUtil redisUtil;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {

    Cookie deleteCookie = new Cookie("REFRESH_TOKEN", null);
    deleteCookie.setMaxAge(0);
    deleteCookie.setPath("/");
    deleteCookie.setSecure(true);
    deleteCookie.setHttpOnly(true);
    response.addCookie(deleteCookie);

    SecurityContextHolder.clearContext();

    String email = resolveEmail(authentication, request);
    if(email != null){
      String redisKey = RedisKeyPolicy.REFRESH_TOKEN.generateKey(email);
      redisUtil.deleteData(redisKey);
      log.info("[로그아웃] Refresh Token 파기 완료, 요청 Id={}", MDC.get("requestId"));
    }

    log.info("로그아웃 성공: 요청 Id={}", MDC.get("requestId"));
  }

  private String resolveEmail(Authentication authentication, HttpServletRequest request) {
    if (authentication != null
        && authentication.getPrincipal() instanceof ModuPlyUserDetails userDetails) {
      return userDetails.getUserDto().getEmail();
    }

    String refreshToken = getCookieValue(request, "REFRESH_TOKEN");
    if (refreshToken == null || refreshToken.isBlank()) {
      return null;
    }

    try {
      return jwtTokenProvider.getEmail(refreshToken);
    } catch (AuthException e) {
      log.warn("[로그아웃] Refresh Token 파싱 실패, 요청 Id={}", MDC.get("requestId"));
      return null;
    }
  }

  private String getCookieValue(HttpServletRequest request, String cookieName){
    Cookie[] cookies = request.getCookies();
    if(cookies != null){
      for(Cookie cookie : cookies){
        if(cookie.getName().equals(cookieName)){
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}
