package com.team6.moduply.auth.controller;

import com.team6.moduply.auth.JwtAuthenticationService;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.dto.JwtDto;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtAuthenticationService jwtAuthenticationService;

  @GetMapping("/csrf-token")
  public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
    csrfToken.getToken();
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refreshAccessToken(
      @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken
  ) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new AuthException(AuthErrorCode.MISSING_TOKEN_EXCEPTION, Map.of(
          "tokenType", "refresh"
      ));
    }

    if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
      throw new AuthException(AuthErrorCode.INVALID_TOKEN_EXCEPTION, Map.of(
          "tokenType", "refresh"
      ));
    }

    UUID userId = jwtTokenProvider.getUserId(refreshToken);
    Authentication authentication = jwtAuthenticationService.getAuthentication(userId);
    String accessToken = jwtTokenProvider.generateAccessToken(authentication);

    ModuPlyUserDetails userDetails = (ModuPlyUserDetails) authentication.getPrincipal();
    JwtDto response = new JwtDto(userDetails.getUserDto(), accessToken);

    // TODO: Redis 기반 refresh token 저장소 도입 후 refresh token 검증 및 rotation 처리 추가
    return ResponseEntity.ok(response);
  }
}
