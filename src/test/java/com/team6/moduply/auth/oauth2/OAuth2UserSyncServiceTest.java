package com.team6.moduply.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2UserSyncServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private SocialAccountRepository socialAccountRepository;

  @Mock
  private OAuth2UserInfo oAuth2UserInfo;

  @Test
  @DisplayName("가입된 사용자가 없으면 사용자와 소셜 계정을 함께 생성한다")
  void sync_creates_user_and_social_account_when_user_not_found() {
    // Given
    OAuth2UserSyncService syncService = new OAuth2UserSyncService(userRepository, socialAccountRepository);
    UUID savedUserId = UUID.randomUUID();
    givenUserInfo("google", "google-provider-id", "tester@example.com", "tester");
    given(userRepository.findByEmail("tester@example.com")).willReturn(Optional.empty());
    doAnswer(invocation -> {
      User user = invocation.getArgument(0);
      ReflectionTestUtils.setField(user, "id", savedUserId);
      return user;
    }).when(userRepository).save(any(User.class));

    // When
    UUID userId = syncService.sync("google", oAuth2UserInfo);

    // Then
    assertThat(userId).isEqualTo(savedUserId);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    ArgumentCaptor<SocialAccount> socialAccountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
    verify(userRepository).save(userCaptor.capture());
    verify(socialAccountRepository).save(socialAccountCaptor.capture());

    User savedUser = userCaptor.getValue();
    SocialAccount savedSocialAccount = socialAccountCaptor.getValue();

    assertThat(savedUser.getEmail()).isEqualTo("tester@example.com");
    assertThat(savedUser.getName()).isEqualTo("tester");
    assertThat(savedUser.getRole()).isEqualTo(Role.USER);
    assertThat(savedSocialAccount.getProvider()).isEqualTo(Provider.GOOGLE);
    assertThat(savedSocialAccount.getProviderId()).isEqualTo("google-provider-id");
    assertThat(savedSocialAccount.getUser()).isSameAs(savedUser);
  }

  @Test
  @DisplayName("가입된 사용자에게 소셜 계정이 없으면 현재 provider로 연동한다")
  void sync_links_social_account_when_user_exists_without_social_account() {
    // Given
    OAuth2UserSyncService syncService = new OAuth2UserSyncService(userRepository, socialAccountRepository);
    UUID userId = UUID.randomUUID();
    User user = user("tester@example.com", Role.USER, false, userId);
    givenUserInfo("kakao", "kakao-provider-id", "tester@example.com", "tester");

    given(userRepository.findByEmail("tester@example.com")).willReturn(Optional.of(user));
    given(socialAccountRepository.findByUserId(userId)).willReturn(Optional.empty());

    // When
    UUID result = syncService.sync("kakao", oAuth2UserInfo);

    // Then
    assertThat(result).isEqualTo(userId);

    ArgumentCaptor<SocialAccount> socialAccountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
    verify(socialAccountRepository).save(socialAccountCaptor.capture());
    assertThat(socialAccountCaptor.getValue().getProvider()).isEqualTo(Provider.KAKAO);
    assertThat(socialAccountCaptor.getValue().getProviderId()).isEqualTo("kakao-provider-id");
    assertThat(socialAccountCaptor.getValue().getUser()).isSameAs(user);
  }

  @Test
  @DisplayName("같은 provider로 이미 연동된 사용자는 기존 사용자 ID를 반환한다")
  void sync_returns_user_id_when_same_provider_already_linked() {
    // Given
    OAuth2UserSyncService syncService = new OAuth2UserSyncService(userRepository, socialAccountRepository);
    UUID userId = UUID.randomUUID();
    User user = user("tester@example.com", Role.USER, false, userId);
    SocialAccount socialAccount = new SocialAccount(Provider.GOOGLE, "google-provider-id", user);
    givenUserInfo("google", "google-provider-id", "tester@example.com", "tester");

    given(userRepository.findByEmail("tester@example.com")).willReturn(Optional.of(user));
    given(socialAccountRepository.findByUserId(userId)).willReturn(Optional.of(socialAccount));

    // When
    UUID result = syncService.sync("google", oAuth2UserInfo);

    // Then
    assertThat(result).isEqualTo(userId);
    verify(socialAccountRepository, never()).save(any(SocialAccount.class));
  }

  @Test
  @DisplayName("잠금 사용자는 OAuth2 로그인 실패 예외를 던진다")
  void sync_throws_oauth2_exception_when_user_is_locked() {
    // Given
    OAuth2UserSyncService syncService = new OAuth2UserSyncService(userRepository, socialAccountRepository);
    User lockedUser = user("tester@example.com", Role.USER, true, UUID.randomUUID());
    givenUserInfo("google", "google-provider-id", "tester@example.com", "tester");
    given(userRepository.findByEmail("tester@example.com")).willReturn(Optional.of(lockedUser));

    // When & Then
    assertThatThrownBy(() -> syncService.sync("google", oAuth2UserInfo))
        .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
            assertThat(exception.getError().getErrorCode())
                .isEqualTo(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION.getCode())
        );

    verify(socialAccountRepository, never()).findByUserId(any(UUID.class));
    verify(socialAccountRepository, never()).save(any(SocialAccount.class));
  }

  @Test
  @DisplayName("이미 다른 provider로 연동된 사용자는 OAuth2 로그인 실패 예외를 던진다")
  void sync_throws_oauth2_exception_when_user_linked_with_different_provider() {
    // Given
    OAuth2UserSyncService syncService = new OAuth2UserSyncService(userRepository, socialAccountRepository);
    UUID userId = UUID.randomUUID();
    User user = user("tester@example.com", Role.USER, false, userId);
    SocialAccount googleAccount = new SocialAccount(Provider.GOOGLE, "google-provider-id", user);
    givenUserInfo("kakao", "kakao-provider-id", "tester@example.com", "tester");

    given(userRepository.findByEmail("tester@example.com")).willReturn(Optional.of(user));
    given(socialAccountRepository.findByUserId(userId)).willReturn(Optional.of(googleAccount));

    // When & Then
    assertThatThrownBy(() -> syncService.sync("kakao", oAuth2UserInfo))
        .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
            assertThat(exception.getError().getErrorCode())
                .isEqualTo(AuthErrorCode.DUPLICATED_ACCOUNT_EXCEPTION.getCode())
        );

    verify(socialAccountRepository, never()).save(any(SocialAccount.class));
  }

  private void givenUserInfo(String provider, String providerId, String email, String nickname) {
    given(oAuth2UserInfo.getProviderId()).willReturn(providerId);
    given(oAuth2UserInfo.getEmail()).willReturn(email);
    given(oAuth2UserInfo.getNickname()).willReturn(nickname);
  }

  private User user(String email, Role role, boolean locked, UUID userId) {
    User user = new User(email, "encoded-password", "tester", role);
    user.updateBlocked(locked);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
