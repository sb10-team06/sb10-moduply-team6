package com.team6.moduply.auth.filter;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.service.AuthService;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.handler.ModuPlyAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
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
      if(SecurityContextHolder.getContext().getAuthentication() == null){
        UUID userId = jwtTokenProvider.getUserId(token);
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
