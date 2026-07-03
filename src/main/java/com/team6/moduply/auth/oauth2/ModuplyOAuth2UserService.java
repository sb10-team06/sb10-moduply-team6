package com.team6.moduply.auth.oauth2;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.oauth2.info.OAuth2UserInfo;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.user.entity.SocialAccount;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.auth.oauth2.enums.Provider;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.SocialAccountRepository;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuplyOAuth2UserService extends DefaultOAuth2UserService {
  private final UserRepository userRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final UserService userService;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // 스프링이 알아서 가져온 구글/카카오 유저 정보 꺼냄
    OAuth2User oAuth2User = super.loadUser(userRequest);
    Map<String, Object> attributes = oAuth2User.getAttributes();

    // 소셜 로그인 공급자 확인
    String provider = userRequest.getClientRegistration().getRegistrationId();
    Provider currentProvider = Provider.fromString(provider);

    OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, attributes);

    String providerId = oAuth2UserInfo.getProviderId();
    String email = oAuth2UserInfo.getEmail();
    String name = oAuth2UserInfo.getNickname();

    // 해당 이메일로 가입된 사용자 있는지 검색
    User user = userRepository.findByEmail(email).orElse(null);

    // 만약 없다면
    if(user == null){
      // 사용자 등록, 소셜 계정 연동
      user = new User(email, "", name, Role.USER);
      SocialAccount socialAccount = new SocialAccount(currentProvider, providerId, user);

      userRepository.save(user);
      socialAccountRepository.save(socialAccount);
    } else{ //있다면
      if (user.isBlocked()) {
        throw new OAuth2AuthenticationException(new OAuth2Error(
            AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION.getCode(),
            AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION.getMessage(),
            null
        ));
      }
      // socialAccount 테이블에서 찾아봄
      Optional<SocialAccount> existingAccount = socialAccountRepository.findByUserId(user.getId());
      // 소셜 계정 연동도 되어있지 않다면 연동
      if(existingAccount.isEmpty()){
        SocialAccount socialAccount = new SocialAccount(currentProvider, providerId, user);
        socialAccountRepository.save(socialAccount);
      } else{ // 계정 연동도 되어있다면
          SocialAccount socialAccount = existingAccount.get();
          // 공급자가 같은지 확인
          if(socialAccount.getProvider() != currentProvider){ // 같지 않다면 예외
            throw new AuthException(AuthErrorCode.DUPLICATED_ACCOUNT_EXCEPTION, Map.of(
                "reason", "같은 이메일의 다른 소셜 계정이 존재합니다.",
                "provider", socialAccount.getProvider().name()
            ));
          }
        // 공급자가 같다면 정상 로그인
        }
      }

    return new ModuPlyUserDetails(userService.getUser(user.getId()), attributes);
  }
}
