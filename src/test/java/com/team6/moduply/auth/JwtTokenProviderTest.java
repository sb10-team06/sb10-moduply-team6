package com.team6.moduply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {
  private static final String SECRET_KEY = "12345678901234567890123456789012";

  private JwtTokenProvider jwtTokenProvider;
  private UUID userId;
  private String email;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", SECRET_KEY);
    ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMinutes", 30);
    ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMinutes", 420);
    jwtTokenProvider.validateSecretKey();

    userId = UUID.randomUUID();
    email = "tester@example.com";
    UserDto userDto = new UserDto(
        userId,
        Instant.now(),
        email,
        "tester",
        null,
        Role.USER,
        false
    );
    ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, "encoded-password");
    authentication = new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );
  }

  @Test
  @DisplayName("Access Token 생성 성공")
  void generate_access_token_success() throws ParseException {
    // When
    String token = jwtTokenProvider.generateAccessToken(authentication, 0L);

    // Then
    assertThat(jwtTokenProvider.validateAccessToken(token)).isTrue();

    JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.getStringClaim("type")).isEqualTo("access");
    assertThat(claims.getStringClaim("email")).isEqualTo(email);
    assertThat(claims.getStringClaim("role")).isNull();
    assertThat(claims.getBooleanClaim("locked")).isNull();
  }

  @Test
  @DisplayName("Refresh Token 생성 성공")
  void generate_refresh_token_success() throws ParseException {
    // When
    String token = jwtTokenProvider.generateRefreshToken(authentication);

    // Then
    assertThat(jwtTokenProvider.validateRefreshToken(token)).isTrue();

    JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.getStringClaim("type")).isEqualTo("refresh");
    assertThat(claims.getStringClaim("email")).isEqualTo(email);
    assertThat(claims.getJWTID()).isNotBlank();
  }

  @Test
  @DisplayName("Refresh Token은 Access Token으로 사용할 수 없다")
  void refresh_token_is_invalid_as_access_token() {
    // Given
    String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    // When & Then
    assertThat(jwtTokenProvider.validateAccessToken(refreshToken)).isFalse();
  }

  @Test
  @DisplayName("만료된 Access Token은 검증에 실패한다")
  void expired_access_token_is_invalid() {
    // Given
    ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMinutes", -1);
    String expiredToken = jwtTokenProvider.generateAccessToken(authentication, 0L);

    // When & Then
    assertThat(jwtTokenProvider.validateAccessToken(expiredToken)).isFalse();
  }

  @Test
  @DisplayName("토큰에서 사용자 ID를 추출한다")
  void get_user_id_success() {
    // Given
    String accessToken = jwtTokenProvider.generateAccessToken(authentication, 0L);

    // When
    UUID result = jwtTokenProvider.getUserId(accessToken);

    // Then
    assertThat(result).isEqualTo(userId);
  }

  @Test
  @DisplayName("토큰에서 사용자 이메일을 추출한다")
  void get_email_success() {
    // Given
    String accessToken = jwtTokenProvider.generateAccessToken(authentication, 0L);

    // When
    String result = jwtTokenProvider.getEmail(accessToken);

    // Then
    assertThat(result).isEqualTo(email);
  }

  @Test
  @DisplayName("토큰의 남은 만료시간을 계산한다")
  void get_remaining_expiration_success() {
    // Given
    String accessToken = jwtTokenProvider.generateAccessToken(authentication, 0L);

    // When
    Duration result = jwtTokenProvider.getRemainingExpiration(accessToken);

    // Then
    assertThat(result).isPositive();
    assertThat(result).isLessThanOrEqualTo(Duration.ofMinutes(30));
  }

  @Test
  @DisplayName("이미 만료된 토큰의 남은 만료시간은 0이다")
  void get_remaining_expiration_returns_zero_when_token_is_expired() {
    // Given
    ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMinutes", -1);
    String expiredToken = jwtTokenProvider.generateAccessToken(authentication, 0L);

    // When
    Duration result = jwtTokenProvider.getRemainingExpiration(expiredToken);

    // Then
    assertThat(result).isZero();
  }

  @Test
  @DisplayName("잘못된 토큰에서 사용자 ID를 추출하면 예외가 발생한다")
  void get_user_id_fail_when_token_is_invalid() {
    // When & Then
    assertThatThrownBy(() -> jwtTokenProvider.getUserId("invalid-token"))
        .isInstanceOfSatisfying(AuthException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN_EXCEPTION)
        );
  }

  @Test
  @DisplayName("잘못된 토큰에서 사용자 이메일을 추출하면 예외가 발생한다")
  void get_email_fail_when_token_is_invalid() {
    // When & Then
    assertThatThrownBy(() -> jwtTokenProvider.getEmail("invalid-token"))
        .isInstanceOfSatisfying(AuthException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN_EXCEPTION)
        );
  }

  @Test
  @DisplayName("JWT 비밀키 길이가 부족하면 예외가 발생한다")
  void validate_secret_key_fail_when_key_is_too_short() {
    // Given
    JwtTokenProvider provider = new JwtTokenProvider();
    ReflectionTestUtils.setField(provider, "secretKey", "short-secret");

    // When & Then
    assertThatThrownBy(provider::validateSecretKey)
        .isInstanceOfSatisfying(AuthException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_JWT_SECRET_KEY_EXCEPTION)
        );
  }
}
