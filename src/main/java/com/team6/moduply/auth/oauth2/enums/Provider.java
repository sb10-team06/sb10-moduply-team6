package com.team6.moduply.auth.oauth2.enums;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Provider {
  GOOGLE, KAKAO;

  public static Provider fromString(String provider) {
    if(provider == null || provider.isBlank()){
      log.error("[보안 경고] OAuth2 가입 요청 중 Provider 값이 비어 있습니다.");
      throw new AuthException(AuthErrorCode.INVALID_PROVIDER_EXCEPTION, Map.of(
          "reason", " Provider 값이 비어 있습니다"
      ));
    }

    return Arrays.stream(Provider.values())
        .filter(p -> p.name().equalsIgnoreCase(provider))
        .findFirst()
        .orElseThrow(() -> {
          log.error("[보안 경고] 지원하지 않는 소셜 로그인 공급자입니다: {}", provider);
          return new AuthException(AuthErrorCode.INVALID_PROVIDER_EXCEPTION, Map.of(
              "reason", "지원하지 않는 소셜 로그인 공급자입니다: " + provider
          ));
        });
  }
}
