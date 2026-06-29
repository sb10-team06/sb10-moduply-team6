package com.team6.moduply.user.event.listener;

import com.team6.moduply.common.config.AsyncConfig;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.event.PasswordChangeEvent;
import com.team6.moduply.user.event.UserLockedEvent;
import com.team6.moduply.user.event.UserUnlockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserSecurityEventListener {
  private final RedisUtil redisUtil;

  @Async(AsyncConfig.USER_SECURITY_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePasswordChangeEvent(PasswordChangeEvent event) {
    // TODO: 보안 이벤트 비동기 처리 실패/지연 시 DB 상태와 Redis 상태가 어긋날 수 있으므로
    //  outbox 또는 내구성 있는 큐 기반 재처리 전략을 검토한다.
    String requestId = MDC.get("requestId");
    log.info("[비밀번호 변경 이벤트 수신] 요청 Id : {} 의 임시 비밀번호 파기를 시작합니다.", requestId);

    try {
      // 1. Redis 임시 비밀번호 Key 조립
      String redisKey = RedisKeyPolicy.PASSWORD_RESET.generateKey(event.getEmail());

      // 2. Redis 데이터 파기
      redisUtil.deleteData(redisKey);

      log.info("[임시 비밀번호 파기 성공] 유저: {}", event.getEmail());

    } catch (Exception e) {
      log.error("[임시 비밀번호 파기 실패] 요청 Id: {}, 원인: {}", requestId, e.getMessage(), e);
    }
  }

  @Async(AsyncConfig.USER_SECURITY_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlerUserLockedEvent(UserLockedEvent event){
    log.info("[유저 정지 이벤트 수신] 요청 Id : {} 의 토큰 무효화 및 블랙리스트 등록을 시작합니다.", MDC.get("requestId"));

    try {
      // 1. Refresh Token 강제 삭제 (재발급 차단)
      // refreshToken 재발급 구현 후 구현 예정

      // 2. Access Token 블랙리스트 등록
      String blacklistKey = RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(event.getEmail());
      redisUtil.setDataExpire(blacklistKey, "locked", RedisKeyPolicy.BLACKLIST_LOCKED.getTtl());

      log.info("[블랙리스트 등록 완료] 요청 Id : {}", MDC.get("requestId"));
    } catch (Exception e) {
      // Redis 통신 에러가 나도 메인 DB 트랜잭션은 롤백되지 않고 이미 성공한 상태를 유지합니다.
      log.error("[블랙리스트 등록 실패] 원인: {}", e.getMessage());
    }
  }

  @Async(AsyncConfig.USER_SECURITY_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlerUserUnlockedEvent(UserUnlockedEvent event) {
    log.info("[유저 정지 해제 이벤트 수신] 요청 Id : {} 의 블랙리스트 삭제를 시작합니다.", MDC.get("requestId"));

    try {
      String blacklistKey = RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(event.getEmail());
      redisUtil.deleteData(blacklistKey);

      log.info("[블랙리스트 삭제 완료] 요청 Id : {}", MDC.get("requestId"));
    } catch (Exception e) {
      log.error("[블랙리스트 삭제 실패] 원인: {}", e.getMessage());
    }
  }

}
