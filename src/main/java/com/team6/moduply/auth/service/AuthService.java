package com.team6.moduply.auth.service;

import com.team6.moduply.auth.dto.ResetPasswordRequest;
import com.team6.moduply.auth.event.TempPasswordEvent;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.TempPasswordUtil;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final TempPasswordUtil tempPasswordUtil;
  private final RoleHierarchy roleHierarchy;

  @Transactional(readOnly = true)
  public Authentication getAuthentication(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN_EXCEPTION, Map.of(
            "userId", userId
        )));

    UserDto userDto = userMapper.toDto(user);
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, user.getEncodedPassword());

    if (!userDetails.isAccountNonLocked()) {
      throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION, Map.of(
          "userId", userId
      ));
    }

    return new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        roleHierarchy.getReachableGrantedAuthorities(userDetails.getAuthorities())
    );
  }

  @Transactional(readOnly = true)
  public void resetPassword(ResetPasswordRequest request) {
    String email = request.getEmail();
    if (!userRepository.existsByEmail(email)) {
      return;
    }

    // 임시 비밀번호 생성
    String tempPassword = tempPasswordUtil.generate(8);
    // TODO: 추후 TTL 기준 시간 고려해볼것
    Duration ttl = RedisKeyPolicy.PASSWORD_RESET.getTtl();
    Instant expiresAt = Instant.now().plus(ttl);

    // 이메일로 발송
    applicationEventPublisher.publishEvent(new TempPasswordEvent(email, tempPassword, expiresAt));
  }
}
