package com.team6.moduply.auth.oauth2.info;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {
  private final Map<String, Object> attributes;
  private final Map<String, Object> kakaoAccount;
  private final Map<String, Object> profile;

  public KakaoUserInfo(Map<String, Object> attributes) {
    this.attributes = attributes;
    this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    this.profile = (Map<String, Object>) kakaoAccount.get("profile");
  }

  @Override
  public String getProvider() {
    return "kakao";
  }

  @Override
  public String getProviderId() {
    // 카카오에서는 provideId가 long으로 넘어와서 String.valueOf()로 변환
    return String.valueOf(attributes.get("id"));
  }

  @Override
  public String getEmail() {
    // 영어 대소문자, 숫자, 한글을 제외한 나머지 모든 단일 문자를 찾아서 _ 로 대체
    String nickname = getNickname().replaceAll("[^a-zA-Z0-9가-힣]", "_");
    return nickname + "_" + getProviderId() + "@kakao.com";
  }

  @Override
  public String getNickname() {
    return (String) profile.get("nickname");
  }
}
