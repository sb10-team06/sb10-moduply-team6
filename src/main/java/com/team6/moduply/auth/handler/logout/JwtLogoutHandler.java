package com.team6.moduply.auth.handler.logout;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.event.UserLogoutEvent;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {
  private static final String SAME_SITE_ATTRIBUTE = "SameSite";
  private static final String SAME_SITE_LAX = "Lax";

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisUtil redisUtil;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final AuthService authService;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {

    Cookie deleteCookie = new Cookie("REFRESH_TOKEN", null);
    deleteCookie.setMaxAge(0);
    deleteCookie.setPath("/");
    deleteCookie.setHttpOnly(true);
    deleteCookie.setSecure(true);
    deleteCookie.setAttribute(SAME_SITE_ATTRIBUTE, SAME_SITE_LAX);
    response.addCookie(deleteCookie);

    SecurityContextHolder.clearContext();

    blacklistAccessToken(request);

    // Refresh Token whitelist와 로그인 세션은 sessionId 기준으로 관리한다.
    // logout 요청이 Access Token만 가지고 오거나 Refresh Token 쿠키만 가지고 와도 가능한 쪽에서 sessionId를 복원한다.
    String sessionId = resolveSessionId(request);
    if (sessionId != null) {
      String redisKey = RedisKeyPolicy.REFRESH_TOKEN.generateKey(sessionId);
      redisUtil.deleteData(redisKey);
      // 남은 Access/Refresh Token이 있더라도 다음 요청에서 세션 ACTIVE 검증에 실패하게 한다.
      authService.revokeSession(sessionId);
      log.info("[로그아웃] Refresh Token 파기 완료, 요청 Id={}", MDC.get("requestId"));
    }
    String email = resolveEmail(authentication, request);
    UUID userId = extractUserId(authentication, request);
    if (userId != null && email != null) {
      applicationEventPublisher.publishEvent(new UserLogoutEvent(userId, email));
      log.info("[로그아웃] 로그아웃 이벤트 발행 완료: userId={}", userId);
    }
    SecurityContextHolder.clearContext();
  }

  // TODO: [김민형] email resolve 메서드와 중복 코드 유틸 메서드로 분리
  private UUID extractUserId(Authentication authentication, HttpServletRequest request) {
    if (authentication != null
        && authentication.getPrincipal() instanceof ModuPlyUserDetails user) {
      return user.getUserDto().getId();
    }
    String refreshToken = getCookieValue(request, "REFRESH_TOKEN");
    if (refreshToken == null || refreshToken.isBlank()) {
      return null;
    }

    try {
      return jwtTokenProvider.getUserId(refreshToken);
    } catch (AuthException e) {
      log.warn("[로그아웃] Refresh Token 파싱 실패, 요청 Id={}", MDC.get("requestId"));
      return null;
    }
  }

  private void blacklistAccessToken(HttpServletRequest request) {
    // 서버가 클라이언트의 Authorization 헤더를 직접 삭제할 수는 없으므로,
    // 로그아웃 요청에 사용된 Access Token을 꺼내 남은 만료시간 동안 blacklist에 등록
    String accessToken = getBearerToken(request);
    if (accessToken == null || accessToken.isBlank()) {
      return;
    }

    try {
      // 여기서는 request header에서 토큰 문자열을 다시 꺼내 Redis에 저장하므로,
      // refresh token/임의 문자열/만료 토큰이 blacklist를 오염시키지 않도록 방어적으로 한 번 더 확인.
      if (!jwtTokenProvider.validateAccessToken(accessToken)) {
        return;
      }

      // blacklist TTL은 Access Token의 남은 유효시간과 동일하게 맞춤
      Duration ttl = jwtTokenProvider.getRemainingExpiration(accessToken);
      authService.blacklistAccessToken(accessToken, ttl);
      log.info("[로그아웃] Access Token 블랙리스트 등록 완료, 요청 Id={}", MDC.get("requestId"));
    } catch (Exception e) {
      log.warn("[로그아웃] Access Token 블랙리스트 등록 실패, 요청 Id={}", MDC.get("requestId"), e);
    }
  }

  private String resolveEmail(Authentication authentication, HttpServletRequest request) {
    // 인증정보에서 이메일을 가져옴
    if (authentication != null
        && authentication.getPrincipal() instanceof ModuPlyUserDetails userDetails) {
      return userDetails.getUserDto().getEmail();
    }

    // 인증정보가 비어있을시 RT에서 가져옴
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

  private String resolveSessionId(HttpServletRequest request) {
    // AT에서 세션id를 가져옴
    String accessToken = getBearerToken(request);
    if (accessToken != null && !accessToken.isBlank()) {
      try {
        return jwtTokenProvider.getSessionId(accessToken);
      } catch (AuthException e) {
        log.warn("[로그아웃] Access Token 세션 파싱 실패, 요청 Id={}", MDC.get("requestId"));
      }
    }

    // AT가 없을 시 RT에서 가져옴
    String refreshToken = getCookieValue(request, "REFRESH_TOKEN");
    if (refreshToken == null || refreshToken.isBlank()) {
      return null;
    }

    try {
      return jwtTokenProvider.getSessionId(refreshToken);
    } catch (AuthException e) {
      log.warn("[로그아웃] Refresh Token 세션 파싱 실패, 요청 Id={}", MDC.get("requestId"));
      return null;
    }
  }

  private String getCookieValue(HttpServletRequest request, String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(cookieName)) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  private String getBearerToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
