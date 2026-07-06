package com.team6.moduply.auth.oauth2;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.oauth2.info.GoogleUserInfo;
import com.team6.moduply.auth.oauth2.info.KakaoUserInfo;
import com.team6.moduply.auth.oauth2.info.OAuth2UserInfo;
import java.util.Map;

public class OAuth2UserInfoFactory {
  public static OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
    return switch (provider.toLowerCase()) {
      case "google" -> new GoogleUserInfo(attributes);
      case "kakao" -> new KakaoUserInfo(attributes);
      default -> throw new AuthException(AuthErrorCode.INVALID_PROVIDER_EXCEPTION, Map.of(
          "provider", provider
      ));
    };
  }

}
