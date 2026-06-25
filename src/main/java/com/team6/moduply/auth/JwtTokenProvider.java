package com.team6.moduply.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {
  private static final int HS256_MIN_SECRET_KEY_BYTES = 32;
  private static final String ACCESS_TOKEN_TYPE = "access";
  private static final String REFRESH_TOKEN_TYPE = "refresh";

  @Value("${jwt.key}")
  private String secretKey;

  @Value("${jwt.access-token-expiration-minutes}")
  private int accessTokenExpirationMinutes;

  @Value("${jwt.refresh-token-expiration-minutes}")
  private int refreshTokenExpirationMinutes;

  private byte[] signingKey;

  // 서버 비밀키의 길이 검증 및 초기화
  @PostConstruct
  public void validateSecretKey() {
    byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < HS256_MIN_SECRET_KEY_BYTES) {
      throw new AuthException(AuthErrorCode.INVALID_JWT_SECRET_KEY_EXCEPTION, Map.of(
          "requiredBytes", HS256_MIN_SECRET_KEY_BYTES,
          "actualBytes", keyBytes.length
      ));
    }
    this.signingKey = keyBytes;
  }

  public String generateAccessToken(Authentication authentication) {
    return generateToken(authentication, ACCESS_TOKEN_TYPE, accessTokenExpirationMinutes);
  }

  public String generateRefreshToken(Authentication authentication) {
    return generateToken(authentication, REFRESH_TOKEN_TYPE, refreshTokenExpirationMinutes);
  }

  public boolean validateAccessToken(String token) {
    return validateToken(token, ACCESS_TOKEN_TYPE);
  }

  public boolean validateRefreshToken(String token) {
    return validateToken(token, REFRESH_TOKEN_TYPE);
  }

  // validateAccessToken 통과 후에만 호출됨
  public Authentication getAuthentication(String token) {
    try{
      // 토큰 준비, 페이로드 준비
      SignedJWT signedJWT = SignedJWT.parse(token);
      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

      // 인증 정보 꺼내옴
      UUID userId = UUID.fromString(claimsSet.getSubject());
      String email = claimsSet.getStringClaim("email");
      Role role = Role.valueOf(claimsSet.getStringClaim("role"));

      SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

      UserDto userDto = new UserDto(userId, null, email, null,null, null, null);
      ModuPlyUserDetails userDetails = new ModuPlyUserDetails(userDto, "");

      return new UsernamePasswordAuthenticationToken(userDetails, null, Collections.singletonList(authority));
    } catch (ParseException e) {
      log.warn("유효하지 않은 JWT 토큰입니다.", e);
      throw new AuthException(AuthErrorCode.INVALID_TOKEN_EXCEPTION, Map.of());
    }
  }

  // 토큰 생성
  private String generateToken(
      Authentication authentication,
      String tokenType,
      int expirationMinutes
  ) {
    try {
      // 서명자 생성
      JWSSigner jwtSigner = new MACSigner(signingKey);
      // 인증 정보에서 정보를 가져옴
      ModuPlyUserDetails userDetails = (ModuPlyUserDetails) authentication.getPrincipal();
      UserDto userDto = userDetails.getUserdto();

      // 발급 시간과 만료 시간 설정
      Date issuedAt = new Date();
      Date expiresAt = new Date(issuedAt.getTime() + expirationMinutes * 60_000L);

      // 공통 페이로드 생성 - 유저 식별id,  토큰 타입, 발급 시간, 만료 시간
      JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
          .subject(userDto.getId().toString())
          .claim("type", tokenType)
          .issueTime(issuedAt)
          .expirationTime(expiresAt);

      // 토큰 타입에 따른 페이로드 추가 정보 설정 - AT이면 권한, 인증정보를 페이로드에 추가
      if (ACCESS_TOKEN_TYPE.equals(tokenType)) {
        claimsBuilder
            .claim("email", userDto.getEmail());
      }

      // 토큰 타입에 따른 페이로드 추가 정보 설정 - RT면 토큰 식별 id(Redis 조회용) 추가
      if (REFRESH_TOKEN_TYPE.equals(tokenType)) {
        claimsBuilder.jwtID(UUID.randomUUID().toString());
      }

      // 서명 생성
      SignedJWT signedJWT = new SignedJWT(
          new JWSHeader(JWSAlgorithm.HS256),
          claimsBuilder.build()
      );
      signedJWT.sign(jwtSigner);

      return signedJWT.serialize();
    } catch (JOSEException | ClassCastException e) {
      log.error("{} token 생성 중 오류 발생", tokenType, e);
      throw new AuthException(AuthErrorCode.TOKEN_GENERATION_FAILED_EXCEPTION, Map.of(
          "tokenType", tokenType
      ));
    }
  }

  private boolean validateToken(String token, String expectedType){
    try{
      // 토큰 nimbus객체로 변환
      SignedJWT signedJWT = SignedJWT.parse(token);
      // 검증기 생성
      JWSVerifier verifier = new MACVerifier(signingKey);

      // 페이로드 가져옴
      JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

      // 해싱 알고리즘 검증
      boolean isExpectedAlgorithm = signedJWT.getHeader().getAlgorithm().equals(JWSAlgorithm.HS256);

      // 서명 검증
      boolean isValidSignature = signedJWT.verify(verifier);

      // 만료일 검증
      Date expirationTime = claims.getExpirationTime();
      boolean isExpired = expirationTime == null || expirationTime.before(new Date());

      // 토큰 타입 검증
      String tokenType = claims.getStringClaim("type");
      boolean isExpectedType = expectedType.equals(tokenType);

      return isExpectedAlgorithm && isValidSignature && isExpectedType && !isExpired;
    } catch (ParseException | JOSEException e) {
      log.warn("유효하지 않은 JWT 토큰입니다.", e);
      return false;
    }
  }
}

