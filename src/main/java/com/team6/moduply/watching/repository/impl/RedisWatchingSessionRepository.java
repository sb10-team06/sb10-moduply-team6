package com.team6.moduply.watching.repository.impl;

import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.model.WatchingSessionResult;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class RedisWatchingSessionRepository implements WatchingSessionRepository {

  private final RedisTemplate<String, WatchingSession> watchingSessionRedisTemplate;
  private final RedisTemplate<String, String> sessionIdAndUserIdRedisTemplate;

  private static final long EXPIRED_HOURS = 12;

  private static final String WATCHER_KEY_PREFIX = "watcher:watcher:";
  private static final String SESSION_KEY_PREFIX = "watcher:session:";
  private static final String CONTENT_KEY_PREFIX = "watcher:content:";

  public RedisWatchingSessionRepository(
      @Qualifier("watchingSessionRedisTemplate") RedisTemplate<String, WatchingSession> watchingSessionRedisTemplate,
      @Qualifier("sessionIdAndUserIdRedisTemplate") RedisTemplate<String, String> sessionIdAndUserIdRedisTemplate) {
    this.watchingSessionRedisTemplate = watchingSessionRedisTemplate;
    this.sessionIdAndUserIdRedisTemplate = sessionIdAndUserIdRedisTemplate;
  }

  @Override
  public WatchingSessionResult save(WatchingSession watchingSession) {
    UUID watcherId = watchingSession.getWatcherId();
    String watcherKey = WATCHER_KEY_PREFIX + watcherId.toString();
    String sessionKey = SESSION_KEY_PREFIX + watchingSession.getSessionId();
    String contentKey = CONTENT_KEY_PREFIX + watchingSession.getContentId().toString();

    WatchingSession previous = watchingSessionRedisTemplate.opsForValue().get(watcherKey);
    if (previous != null && !previous.getSessionId().equals(watchingSession.getSessionId())) {
      sessionIdAndUserIdRedisTemplate.delete(SESSION_KEY_PREFIX + previous.getSessionId());

      // 과거 컨텐츠 인덱스가 0이 되면 삭제
      String prevContentKey = CONTENT_KEY_PREFIX + previous.getContentId();
      sessionIdAndUserIdRedisTemplate.opsForSet().remove(prevContentKey, watcherId.toString());

      Long prevCount = countByContentId(previous.getContentId());
      if (prevCount == 0) {
        sessionIdAndUserIdRedisTemplate.delete(prevContentKey);
      }
    }

    watchingSessionRedisTemplate.opsForValue()
        .set(watcherKey, watchingSession, EXPIRED_HOURS, TimeUnit.HOURS);
    sessionIdAndUserIdRedisTemplate.opsForValue()
        .set(sessionKey, watcherId.toString(), EXPIRED_HOURS, TimeUnit.HOURS);
    sessionIdAndUserIdRedisTemplate.opsForSet()
        .add(contentKey, watchingSession.getWatcherId().toString());

    return new WatchingSessionResult(watchingSession,
        countByContentId(watchingSession.getContentId()));
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
  public Optional<WatchingSessionResult> deleteBySessionId(String sessionId) {
    String sessionKey = SESSION_KEY_PREFIX + sessionId;
    String watcherId = sessionIdAndUserIdRedisTemplate.opsForValue().get(sessionKey);

    if (watcherId == null) {
      return Optional.empty();
    }

    String watcherKey = WATCHER_KEY_PREFIX + watcherId;
    WatchingSession session = watchingSessionRedisTemplate.opsForValue().get(watcherKey);

    if (session != null && session.getSessionId().equals(sessionId)) {
      String contentKey = CONTENT_KEY_PREFIX + session.getContentId();
      sessionIdAndUserIdRedisTemplate.delete(sessionKey);
      watchingSessionRedisTemplate.delete(watcherKey);
      sessionIdAndUserIdRedisTemplate.opsForSet()
          .remove(contentKey, session.getWatcherId().toString());

      long currentCount = countByContentId(session.getContentId());
      if (currentCount == 0) {
        sessionIdAndUserIdRedisTemplate.delete(contentKey);
      }
      return Optional.of(new WatchingSessionResult(session, currentCount));
    }

    sessionIdAndUserIdRedisTemplate.delete(sessionKey);
    return Optional.empty();
  }

  @Override
  public long countByContentId(UUID contentId) {
    String contentKey = CONTENT_KEY_PREFIX + contentId.toString();
    Long count = sessionIdAndUserIdRedisTemplate.opsForSet().size(contentKey);
    return count == null ? 0 : count;
  }

}
