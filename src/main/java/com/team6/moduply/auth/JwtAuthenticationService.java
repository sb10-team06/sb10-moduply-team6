package com.team6.moduply.auth;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

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
}
