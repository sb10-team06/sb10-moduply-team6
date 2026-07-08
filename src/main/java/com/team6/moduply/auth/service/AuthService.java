package com.team6.moduply.auth.service;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.dto.JwtDto;
import com.team6.moduply.auth.dto.ResetPasswordRequest;
import com.team6.moduply.auth.dto.TokenRefreshDto;
import com.team6.moduply.auth.event.TempPasswordEvent;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.auth.util.RefreshTokenRedisUtil;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.common.util.TempPasswordUtil;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
  private static final String BLACKLIST_ACCESS_TOKEN_VALUE = "logout";
  private static final String SHA_256 = "SHA-256";

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final TempPasswordUtil tempPasswordUtil;
  private final BinaryContentService binaryContentService;
  private final RoleHierarchy roleHierarchy;
  private final RefreshTokenRedisUtil refreshTokenRedisUtil;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisUtil redisUtil;

  @Transactional(readOnly = true)
  public Authentication getAuthentication(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_TOKEN_EXCEPTION, Map.of(
            "userId", userId
        )));

    UserDto userDto = toDto(user);
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, user.getEncodedPassword());

    if (!userDetails.isAccountNonLocked()) {
      throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED_EXCEPTION, Map.of(
          "userId", userId
      ));
    }

    return new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        // TODO: RoleHierarchy 적용 위치를 인증 객체와 인가 설정 중 어디에 둘지
        //  WebSocket/HTTP 전체 흐름을 보고 리팩토링 단계에서 다시 정리한다.
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

  @Transactional
  public TokenRefreshDto refreshTokens(String refreshToken){
    if(!jwtTokenProvider.validateRefreshToken(refreshToken)){
      throw new AuthException(AuthErrorCode.INVALID_TOKEN_EXCEPTION, Map.of(
          "tokenType", "refresh"
      ));
    }

    String email = jwtTokenProvider.getEmail(refreshToken);
    Authentication authentication = getAuthentication(jwtTokenProvider.getUserId(refreshToken));

    String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    String result = refreshTokenRedisUtil.rotate(email, refreshToken, newRefreshToken);
    if(!"OK".equals(result)){
      log.error("[보안 비상] refresh token 갱신 실패. result={}, 요청 Id={}", result, MDC.get("requestId"));
      throw new AuthException(AuthErrorCode.COMPROMISED_TOKEN_EXCEPTION, Map.of(
          "tokenType", "refresh"
      ));
    }

    ModuPlyUserDetails userDetails = (ModuPlyUserDetails) authentication.getPrincipal();
    return new TokenRefreshDto(new JwtDto(userDetails.getUserDto(), newAccessToken), newRefreshToken);
  }

  public void blacklistAccessToken(String accessToken, Duration ttl) {
    // 엑세스 토큰이 비어있거나 만료기간이 지났거나 없으면 반환
    if (accessToken == null || accessToken.isBlank() || ttl == null || ttl.isZero()
        || ttl.isNegative()) {
      return;
    }

    // 만료기간이 남은 엑세스 토큰으로 만료 잔여시간만큼 redis에 블랙리스트 등록
    redisUtil.setDataExpire(
        // 해싱된 토큰으로 저장
        RedisKeyPolicy.BLACKLIST_ACCESS_TOKEN.generateKey(hashToken(accessToken)),
        BLACKLIST_ACCESS_TOKEN_VALUE,
        ttl
    );
  }

  // 블랙리스트에 엑세스 토큰이 있는지 확인
  public boolean isAccessTokenBlacklisted(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return false;
    }

    // 해싱된 토큰으로 조회
    String redisKey = RedisKeyPolicy.BLACKLIST_ACCESS_TOKEN.generateKey(hashToken(accessToken));
    return redisUtil.getData(redisKey) != null;
  }

  private UserDto toDto(User user) {
    // TODO: 인증 객체 생성과 응답용 프로필 URL 생성 책임이 섞여 있으므로 추후 분리 리팩토링 필요
    String profileImageUrl = binaryContentService.generateUrl(user.getProfileImg());
    return userMapper.toDto(user, profileImageUrl);
  }

  // redis에 원문으로 access token을 저장하지 않기 위해 해싱 진행
  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance(SHA_256);
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available.", e);
    }
  }
}
