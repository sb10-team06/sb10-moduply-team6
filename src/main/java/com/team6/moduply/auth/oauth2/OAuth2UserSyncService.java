package com.team6.moduply.auth.oauth2;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.oauth2.enums.Provider;
import com.team6.moduply.auth.oauth2.info.OAuth2UserInfo;
import com.team6.moduply.user.entity.SocialAccount;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.SocialAccountRepository;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2UserSyncService {

  private final UserRepository userRepository;
  private final SocialAccountRepository socialAccountRepository;

  @Transactional
  public UUID sync(String provider, OAuth2UserInfo oAuth2UserInfo) {
    Provider currentProvider = Provider.fromString(provider);
    String providerId = oAuth2UserInfo.getProviderId();
    String email = oAuth2UserInfo.getEmail();
    String name = oAuth2UserInfo.getNickname();

    User user = userRepository.findByEmail(email).orElse(null);

    // 가입된 사용자가 없으면 소셜 로그인 정보로 새 사용자와 소셜 계정을 함께 생성한다.
    if (user == null) {
      user = new User(email, "", name, Role.USER);
      SocialAccount socialAccount = new SocialAccount(currentProvider, providerId, user);

      userRepository.save(user);
      socialAccountRepository.save(socialAccount);
      return user.getId();
    }

    // 가입된 사용자가 잠금 상태이면 OAuth2 로그인 실패 흐름으로 넘긴다.
    if (user.isBlocked()) {
      oauth2Exception(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION);
    }

    Optional<SocialAccount> existingAccount = socialAccountRepository.findByUserId(user.getId());

    // 가입된 사용자는 있지만 소셜 계정 연동이 없으면 현재 provider로 연동한다.
    if (existingAccount.isEmpty()) {
      SocialAccount socialAccount = new SocialAccount(currentProvider, providerId, user);
      socialAccountRepository.save(socialAccount);
      return user.getId();
    }

    SocialAccount socialAccount = existingAccount.get();

    // 이미 다른 provider로 연동된 계정이면 OAuth2 로그인 실패 흐름으로 넘긴다.
    if (socialAccount.getProvider() != currentProvider) {
      oauth2Exception(AuthErrorCode.DUPLICATED_ACCOUNT_EXCEPTION);
    }

    // 같은 provider로 이미 연동된 계정이면 그대로 로그인한다.
    return user.getId();
  }

  private void oauth2Exception(AuthErrorCode errorCode) {
    throw new OAuth2AuthenticationException(new OAuth2Error(
        errorCode.getCode(),
        errorCode.getMessage(),
        null
    ));
  }
}
