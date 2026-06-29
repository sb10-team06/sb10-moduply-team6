package com.team6.moduply.user.event.listener;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.event.PasswordChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordChangeListener {
  private final RedisUtil redisUtil;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePasswordChangeEvent(PasswordChangeEvent event) {
    // redis 임시 비밀번호 파기
    String redisKey = RedisKeyPolicy.PASSWORD_RESET.generateKey(event.getEmail());
    redisUtil.deleteData(redisKey);
  }

}
