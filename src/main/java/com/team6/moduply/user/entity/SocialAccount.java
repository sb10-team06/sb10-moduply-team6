package com.team6.moduply.user.entity;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import com.team6.moduply.auth.oauth2.enums.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "social_accounts",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SocialAccount extends BaseUpdatableEntity {
  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false)
  private Provider provider;

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  public SocialAccount(Provider provider, String providerId, User user) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.user = Objects.requireNonNull(user, "user must not be null");
    // 추후 커스텀 예외로 변경
    if (!StringUtils.hasText(providerId)) {
      throw new AuthException(AuthErrorCode.INVALID_PROVIDER_EXCEPTION, Map.of(
          "reason", "providerId must not be blank"
      ));
    }

    this.providerId = providerId;
  }
}
