package com.team6.moduply.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.oauth2.info.OAuth2UserInfo;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuth2UserInfoFactoryTest {

  @Test
  @DisplayName("Google 사용자 정보에서 providerId, email, nickname을 추출한다")
  void get_oauth2_user_info_google() {
    // Given
    Map<String, Object> attributes = Map.of(
        "sub", "google-provider-id",
        "email", "tester@example.com",
        "name", "tester"
    );

    // When
    OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributes);

    // Then
    assertThat(userInfo.getProvider()).isEqualTo("GOOGLE");
    assertThat(userInfo.getProviderId()).isEqualTo("google-provider-id");
    assertThat(userInfo.getEmail()).isEqualTo("tester@example.com");
    assertThat(userInfo.getNickname()).isEqualTo("tester");
  }

  @Test
  @DisplayName("Kakao 사용자 정보에서 가상 이메일을 생성한다")
  void get_oauth2_user_info_kakao() {
    // Given
    Map<String, Object> attributes = Map.of(
        "id", 12345L,
        "kakao_account", Map.of(
            "profile", Map.of(
                "nickname", "카카오 닉!"
            )
        )
    );

    // When
    OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("kakao", attributes);

    // Then
    assertThat(userInfo.getProvider()).isEqualTo("KAKAO");
    assertThat(userInfo.getProviderId()).isEqualTo("12345");
    assertThat(userInfo.getEmail()).isEqualTo("카카오_닉__12345@kakao.com");
    assertThat(userInfo.getNickname()).isEqualTo("카카오 닉!");
  }

  @Test
  @DisplayName("지원하지 않는 소셜 로그인 공급자이면 예외가 발생한다")
  void get_oauth2_user_info_fail_when_provider_unsupported() {
    // When & Then
    assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo("unknown", Map.of()))
        .isInstanceOf(AuthException.class)
        .extracting("errorCode")
        .isEqualTo(AuthErrorCode.INVALID_PROVIDER_EXCEPTION);
  }
}
