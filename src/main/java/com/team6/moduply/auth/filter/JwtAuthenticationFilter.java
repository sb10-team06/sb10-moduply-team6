package com.team6.moduply.auth.filter;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.handler.exception.ModuPlyAuthenticationEntryPoint;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
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
        throw new BadCredentialsException("로그아웃된 엑세스 토큰입니다.");
      }
      if(SecurityContextHolder.getContext().getAuthentication() == null){
        UUID userId = jwtTokenProvider.getUserId(token);
        String email = jwtTokenProvider.getEmail(token);
        long tokenVersion = jwtTokenProvider.getTokenVersion(token);
        String blacklistKey = RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(email);
        // 권한 변경으로 버전이 올라간 사용자의 기존 Access Token을 차단한다.
        if(!validateTokenVersion(tokenVersion, email)){
          throw new BadCredentialsException("토큰 버전이 유효하지 않습니다. 다시 로그인 해주세요.");
        }
        if (redisUtil.getData(blacklistKey) != null) {
          log.warn("[보안 경고] 정지된 계정의 접근 시도입니다. userId={}", userId);

          throw new LockedException("정지된 계정입니다. 관리자에게 문의하세요.");
        }

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

  private boolean validateTokenVersion(long jwtTokenVersion, String email){
    String tokenVersionKey = RedisKeyPolicy.USER_TOKEN_VERSION.generateKey(email);
    String redisTokenVersion = redisUtil.getData(tokenVersionKey);

    if(redisTokenVersion == null){
      return false;
    }
    try{
      return jwtTokenVersion == Long.parseLong(redisTokenVersion);
    } catch (NumberFormatException e){
      return false;
    }
  }
}
