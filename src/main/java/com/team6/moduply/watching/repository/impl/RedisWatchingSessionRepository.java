package com.team6.moduply.watching.repository.impl;

import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 시청 세션(WatchingSession)을 관리하는 Redis 저장소 구현체입니다.
 *
 * <p><b>주요 특징:</b></p>
 * <ul>
 *   <li>다중 서버 인스턴스 환경에서 분산 세션 메모리를 공유합니다.</li>
 *   <li>세션의 기본 만료 시간(TTL)은 12시간으로 제한하여 고스트 세션을 방지합니다.</li>
 *   <li>웹소켓 연결 해제 이벤트 발생 시 사용자 ID 유실에 대비하여,
 *       세션 ID와 사용자 ID를 매핑하는 보조 인덱스를 함께 관리합니다.</li>
 * </ul>
 *
 * @author 김민형
 * @since 26. 6. 24.
 */
@RequiredArgsConstructor
public class RedisWatchingSessionRepository implements WatchingSessionRepository {

  private final RedisTemplate<String, WatchingSession> watchingSessionRedisTemplate;
  private final RedisTemplate<String, String> sessionIdAndUserIdRedisTemplate;

  private static final long EXPIRED_HOURS = 12;

  private static final String WATCHER_KEY_PREFIX = "watcher:watcher:";
  private static final String SESSION_KEY_PREFIX = "watcher:session:";

  @Override
  public WatchingSession save(WatchingSession watchingSession) {
    UUID watcherId = watchingSession.getWatcherId();
    String watcherKey = WATCHER_KEY_PREFIX + watcherId.toString();
    String sessionKey = SESSION_KEY_PREFIX + watchingSession.getSessionId();

    WatchingSession previous = watchingSessionRedisTemplate.opsForValue().get(watcherKey);
    if (previous != null && !previous.getSessionId().equals(watchingSession.getSessionId())) {
      sessionIdAndUserIdRedisTemplate.delete(SESSION_KEY_PREFIX + previous.getSessionId());
    }

    watchingSessionRedisTemplate.opsForValue()
        .set(watcherKey, watchingSession, EXPIRED_HOURS, TimeUnit.HOURS);
    sessionIdAndUserIdRedisTemplate.opsForValue()
        .set(sessionKey, watcherId.toString(), EXPIRED_HOURS, TimeUnit.HOURS);

    return watchingSession;
  }

  @Override
  public Optional<WatchingSession> findByUserId(UUID userId) {
    String watcherKey = WATCHER_KEY_PREFIX + userId.toString();
    return Optional.ofNullable(watchingSessionRedisTemplate.opsForValue().get(watcherKey));
  }

  @Override
  public Optional<WatchingSession> findBySessionId(String sessionId) {
    String sessionKey = SESSION_KEY_PREFIX + sessionId;
    String watcherId = sessionIdAndUserIdRedisTemplate.opsForValue().get(sessionKey);
    if (watcherId == null) {
      return Optional.empty();
    }
    String watcherKey = WATCHER_KEY_PREFIX + watcherId;
    return Optional.ofNullable(watchingSessionRedisTemplate.opsForValue().get(watcherKey));
  }

  @Override
  public void deleteBySessionId(String sessionId) {
    String sessionKey = SESSION_KEY_PREFIX + sessionId;
    String watcherId = sessionIdAndUserIdRedisTemplate.opsForValue().get(sessionKey);

    if (watcherId != null) {
      String watcherKey = WATCHER_KEY_PREFIX + watcherId;
      sessionIdAndUserIdRedisTemplate.delete(sessionKey);
      WatchingSession session = watchingSessionRedisTemplate.opsForValue().get(watcherKey);
      if (session != null && session.getSessionId().equals(sessionId)) {
        watchingSessionRedisTemplate.delete(watcherKey);
      }
    }
  }
}
