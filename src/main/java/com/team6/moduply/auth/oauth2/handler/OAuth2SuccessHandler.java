package com.team6.moduply.auth.oauth2.handler;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.dto.UserDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisUtil redisUtil;
  @Value("${jwt.refresh-token-expiration-minutes}")
  private int refreshTokenExpirationMinutes;
  @Value("${jwt.oauth-success-url}")
  private String redirectUrl;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    ModuPlyUserDetails userDetails = (ModuPlyUserDetails) authentication.getPrincipal();

    UserDto userDto = userDetails.getUserDto();

    // OAuth 로그인도 토큰 재발급 전에 사용자 토큰 버전을 준비한다.
    String tokenVersionKey =
        RedisKeyPolicy.USER_TOKEN_VERSION.generateKey(userDto.getEmail());
    redisUtil.setDataIfAbsent(tokenVersionKey, "0");

    String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    String redisKey = RedisKeyPolicy.REFRESH_TOKEN.generateKey(userDto.getEmail());
    redisUtil.setDataExpire(redisKey, refreshToken, RedisKeyPolicy.REFRESH_TOKEN.getTtl());

    ResponseCookie responseCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
        .httpOnly(true)
        .path("/").sameSite("Lax")
        .maxAge(refreshTokenExpirationMinutes * 60)
                        .build();

    response.addHeader("Set-Cookie", responseCookie.toString());

    // Access Token은 URL에 노출하지 않고, Refresh Token 쿠키 기반 재발급 흐름으로 복원한다.
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
