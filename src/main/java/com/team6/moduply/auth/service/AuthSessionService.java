package com.team6.moduply.auth.service;

import com.team6.moduply.auth.util.AuthSessionRedisUtil;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

  private static final String ACTIVE = "ACTIVE";
  private static final String REVOKED = "REVOKED";

  private final RedisUtil redisUtil;
  private final AuthSessionRedisUtil authSessionRedisUtil;

  /**
   * 로그인 성공 시 새로운 인증 세션을 생성한다.
   *
   * <p>중복 세션 방지 정책은 "같은 사용자 + 같은 브라우저" 단위로 적용한다. 따라서 같은
   * browserId로 다시 로그인하면 이전 sessionId를 REVOKED 처리하고 새 sessionId만 ACTIVE로 둔다.
   * 다른 브라우저는 browserId가 다르므로 기존 세션을 유지한다.</p>
   *
   * <p>Redis에는 두 종류의 키를 쓴다.
   * <ul>
   *   <li>{@code auth:session:{sessionId}}: 실제 세션 상태(ACTIVE/REVOKED)</li>
   *   <li>{@code auth:user-browser-session:{userId}:{browserId}}: 해당 브라우저의 현재 sessionId 인덱스</li>
   * </ul>
   * </p>
   */
  public String createSession(UUID userId, String browserId) {
    String sessionId = UUID.randomUUID().toString();

    authSessionRedisUtil.createSession(userId, browserId, sessionId);

    return sessionId;
  }

  public boolean isSessionActive(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return false;
    }
    return ACTIVE.equals(redisUtil.getData(sessionKey(sessionId)));
  }

  public void revokeSession(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    // 삭제하지 않고 REVOKED를 남기는 이유:
    // - 키 없음: 만료/존재하지 않음
    // - REVOKED: 명시적으로 폐기됨
    // 을 구분할 수 있어 중복 로그인/로그아웃 디버깅이 쉽다.
    redisUtil.setDataExpire(
        sessionKey(sessionId),
        REVOKED,
        RedisKeyPolicy.AUTH_SESSION.getTtl()
    );
  }

  private String sessionKey(String sessionId) {
    return RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId);
  }
}
