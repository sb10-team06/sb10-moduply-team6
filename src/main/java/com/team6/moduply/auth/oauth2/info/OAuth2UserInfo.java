package com.team6.moduply.auth.oauth2.info;

public interface OAuth2UserInfo {
  String getProvider();
  String getProviderId();
  String getEmail();
  String getNickname();
}
