package com.team6.moduply.common.enums;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Redis 데이터 저장을 위한 전사(Global) 키 정책 관리 Enum입니다.
 *
 * [도입 목적]
 * 1. 키 충돌 방지: 도메인별로 고유한 접두사(Prefix)를 강제하여, 서로 다른 데이터가 덮어씌워지는 대참사를 막습니다.
 * 2. 하드코딩 제거: 코드 곳곳에 흩어진 "문자열"과 만료 시간(숫자)을 한 곳에서 중앙 통제하여 유지보수성을 높입니다.
 *
 * -----------------------------------------------------------------
 * 🛠️ [새로운 캐시 정책 추가하는 방법]
 * 1. 아래에 Enum 상수를 하나 새로 만듭니다. (대문자 스네이크 케이스 권장)
 * 2. 사용할 '고유 접두사(Prefix)'와 '만료 시간(TTL)'을 괄호 안에 세팅합니다.
 * (주의: 접두사 끝에는 콜론(:)을 붙여 네임스페이스를 구분해 주세요!)
 * * * 작성 예시:
 * EMAIL_VERIFICATION("email:verify:", Duration.ofMinutes(5))
 * -----------------------------------------------------------------
 */
@Getter
@AllArgsConstructor
public enum RedisKeyPolicy {
  PASSWORD_RESET("reset:", Duration.ofMinutes(3)),
  REFRESH_TOKEN("refresh:", Duration.ofHours(7)),
  BLACKLIST_ACCESS_TOKEN("blacklist:access:", Duration.ZERO),
  BLACKLIST_LOCKED("blacklist:locked:", Duration.ofHours(1)),
  USER_TOKEN_VERSION("access:token-version:", Duration.ZERO),
  AUTH_SESSION("auth:session:", Duration.ofHours(7)),
  USER_BROWSER_SESSION("auth:user-browser-session:", Duration.ofHours(7));

  // 추후에 목적에 맞는 레디스 키를 추가


  private final String prefix;
  private final Duration ttl;

  public String generateKey(String identifier) {
    return prefix + identifier;
  }
}
