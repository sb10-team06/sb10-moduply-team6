package com.team6.moduply.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.dto.JwtDto;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.user.dto.UserDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;
  private final CsrfTokenRepository csrfTokenRepository;
  @Value("${jwt.refresh-token-expiration-minutes}")
  private int refreshTokenExpirationMinutes;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    // 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(authentication);
    String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    ModuPlyUserDetails userDetails = (ModuPlyUserDetails) authentication.getPrincipal();
    UserDto userDto = userDetails.getUserDto();

    // http 헤더 설정
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    // Authorization헤더에 Bearer을 붙혀서 토큰임을 알림
    response.setHeader("Authorization", "Bearer " + accessToken);

    // 쿠키에 refreshToken 저장
    Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setPath("/");
    refreshTokenCookie.setSecure(true);
    refreshTokenCookie.setMaxAge(refreshTokenExpirationMinutes * 60);
    response.addCookie(refreshTokenCookie);

    CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
    csrfTokenRepository.saveToken(csrfToken, request, response);

    objectMapper.writeValue(response.getWriter(), new JwtDto(userDto, accessToken));
  }
}
