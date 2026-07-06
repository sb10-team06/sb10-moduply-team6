package com.team6.moduply.auth.oauth2;

import com.team6.moduply.auth.oauth2.info.OAuth2UserInfo;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.service.UserService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuplyOAuth2UserService extends DefaultOAuth2UserService {
  private final OAuth2UserSyncService oAuth2UserSyncService;
  private final UserService userService;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // 스프링이 알아서 가져온 구글/카카오 유저 정보 꺼냄
    OAuth2User oAuth2User = super.loadUser(userRequest);
    Map<String, Object> attributes = oAuth2User.getAttributes();

    // 소셜 로그인 공급자 확인
    String provider = userRequest.getClientRegistration().getRegistrationId();
    OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, attributes);

    UUID userId = oAuth2UserSyncService.sync(provider, oAuth2UserInfo);
    UserDto userDto = userService.getUser(userId);

    return new ModuPlyUserDetails(userDto, attributes);
  }
}
