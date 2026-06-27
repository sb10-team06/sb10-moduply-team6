package com.team6.moduply.auth.service;

import com.team6.moduply.auth.dto.ResetPasswordRequest;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.common.util.TempPasswordUtil;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
  private final RedisUtil redisUtil;
  private final TempPasswordUtil tempPasswordUtil;

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
        userDetails.getAuthorities()
    );
  }

  @Transactional(readOnly = true)
  public void resetPassword(ResetPasswordRequest request) {
    if(!userRepository.existsByEmail(request.getEmail())) {
      throw new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of("email", request.getEmail()));
    }

    // 임시 비밀번호 생성
    String tempPassword = tempPasswordUtil.generate(8);

    redisUtil.setDateExpire("");

    // 이메일로 발송

  }
}
