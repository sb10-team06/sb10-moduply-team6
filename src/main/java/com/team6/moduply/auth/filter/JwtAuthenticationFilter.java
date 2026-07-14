package com.team6.moduply.auth.filter;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.handler.exception.ModuPlyAuthenticationEntryPoint;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthService authService;
  private final ModuPlyAuthenticationEntryPoint moduPlyAuthenticationEntryPoint;
  private final RedisUtil redisUtil;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = getJwtToken(request);

    if(token == null){
      filterChain.doFilter(request, response);
      return;
    }
    try{
      if (!jwtTokenProvider.validateAccessToken(token)) {
        throw new BadCredentialsException("엑세스 토큰이 유효하지 않습니다.");
      }
      if (authService.isAccessTokenBlacklisted(token)) {
        throw new BadCredentialsException(
            "로그아웃된 엑세스 토큰입니다.",
            new AuthException(AuthErrorCode.BLACKLISTED_TOKEN_EXCEPTION, Map.of(
                "reason", "access token blacklisted"
            ))
        );
      }
      UUID userId = jwtTokenProvider.getUserId(token);
      String email = jwtTokenProvider.getEmail(token);
      long tokenVersion = jwtTokenProvider.getTokenVersion(token);
      String sessionId = jwtTokenProvider.getSessionId(token);
      String blacklistKey = RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(email);

      // JWT 인증은 stateless이므로 Redis 기반 상태 검증은 매 요청마다 수행해야 한다.
      // SecurityContext에 이미 인증 객체가 있더라도 세션 폐기/권한 변경/계정 정지 상태는
      // 요청 시점의 최신 Redis 값을 기준으로 다시 확인한다.
      if(!authService.isTokenVersionValid(email, tokenVersion)){
        throw new BadCredentialsException(
            "토큰 버전이 유효하지 않습니다. 다시 로그인 해주세요.",
            new AuthException(AuthErrorCode.TOKEN_VERSION_MISMATCH_EXCEPTION, Map.of(
                "userId", userId,
                "email", email,
                "tokenVersion", tokenVersion
            ))
        );
      }
      // Access Token의 sessionId가 Redis에서 ACTIVE일 때만 요청을 허용한다.
      // 같은 브라우저에서 다시 로그인하면 이전 tokenVersion은 그대로여도 sessionId가 REVOKED 되므로 여기서 차단된다.
      if (!authService.isSessionActive(sessionId)) {
        throw new BadCredentialsException(
            "로그인 세션이 유효하지 않습니다. 다시 로그인 해주세요.",
            new AuthException(AuthErrorCode.SESSION_REVOKED_EXCEPTION, Map.of(
                "userId", userId,
                "email", email,
                "sessionId", sessionId
            ))
        );
      }
      if (redisUtil.getData(blacklistKey) != null) {
        throw new LockedException("정지된 계정입니다. 관리자에게 문의하세요.");
      }

      if(SecurityContextHolder.getContext().getAuthentication() == null){
        Authentication authentication = authService.getAuthentication(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }

      filterChain.doFilter(request, response);
    } catch (AuthenticationException e) {
      SecurityContextHolder.clearContext();
      moduPlyAuthenticationEntryPoint.commence(request, response, e);
    } catch (AuthException e) {
      SecurityContextHolder.clearContext();
      moduPlyAuthenticationEntryPoint.commence(request,
          response,
          new BadCredentialsException(e.getMessage(),e));
    }

  }

  private String getJwtToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

}
