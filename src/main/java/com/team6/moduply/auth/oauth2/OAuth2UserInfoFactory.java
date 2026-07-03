package com.team6.moduply.auth.oauth2;

import com.team6.moduply.auth.oauth2.info.GoogleUserInfo;
import com.team6.moduply.auth.oauth2.info.KakaoUserInfo;
import com.team6.moduply.auth.oauth2.info.OAuth2UserInfo;
import java.util.Map;

public class OAuth2UserInfoFactory {
  public static OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
    return switch (provider.toLowerCase()) {
      case "google" -> new GoogleUserInfo(attributes);
      case "kakao" -> new KakaoUserInfo(attributes);
      default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인 공급자입니다: " + provider);
    };
  }

}
