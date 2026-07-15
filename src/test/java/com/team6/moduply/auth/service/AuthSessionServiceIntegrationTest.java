package com.team6.moduply.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.config.support.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class AuthSessionServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private AuthSessionService authSessionService;

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  @Test
  @DisplayName("같은 브라우저에서 동시 로그인해도 최종 세션 하나만 ACTIVE로 남는다")
  void create_session_keeps_only_latest_session_active_under_concurrent_same_browser_login()
      throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    String browserId = UUID.randomUUID().toString();
    int loginCount = 20;
    ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
    CountDownLatch ready = new CountDownLatch(loginCount);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < loginCount; i++) {
      futures.add(executorService.submit(() -> {
        ready.countDown();
        start.await();
        return authSessionService.createSession(userId, browserId);
      }));
    }

    try {
      ready.await();

      // When
      start.countDown();

      List<String> sessionIds = new ArrayList<>();
      for (Future<String> future : futures) {
        sessionIds.add(future.get());
      }

      // Then
      String browserSessionKey = RedisKeyPolicy.USER_BROWSER_SESSION.generateKey(
          userId + ":" + browserId
      );
      String currentSessionId = stringRedisTemplate.opsForValue().get(browserSessionKey);

      assertThat(currentSessionId).isIn(sessionIds);

      for (String sessionId : sessionIds) {
        String sessionStatus = stringRedisTemplate.opsForValue()
            .get(RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId));

        if (sessionId.equals(currentSessionId)) {
          assertThat(sessionStatus).isEqualTo("ACTIVE");
        } else {
          assertThat(sessionStatus).isEqualTo("REVOKED");
        }
      }
    } finally {
      executorService.shutdownNow();
      cleanupSessions(userId, browserId, futures);
    }
  }

  private void cleanupSessions(UUID userId, String browserId, List<Future<String>> futures) {
    stringRedisTemplate.delete(RedisKeyPolicy.USER_BROWSER_SESSION.generateKey(userId + ":" + browserId));

    for (Future<String> future : futures) {
      if (!future.isDone()) {
        continue;
      }

      try {
        stringRedisTemplate.delete(RedisKeyPolicy.AUTH_SESSION.generateKey(future.get()));
      } catch (Exception ignored) {
        // 테스트 정리 과정에서 실패한 Future는 삭제할 세션 ID가 없으므로 무시한다.
      }
    }
  }
}
