package com.team6.moduply.watching.repository.impl;

import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 시청 세션(WatchingSession)을 관리하는 인메모리 저장소 구현체입니다.
 *
 * <p><b>주요 특징:</b></p>
 * <ul>
 *   <li>{@link ConcurrentHashMap}을 사용하여 멀티스레드 환경에서 Thread-Safe를 보장합니다.</li>
 *   <li>30분 단위로 만료된 시청 세션을 삭제하며, 만료시간은 12시간입니다.</li>
 *   <li>웹소켓 연결 해제 이벤트 발생 시 사용자 ID 유실에 대비하여,
 *       세션 ID와 사용자 ID를 매핑하는 보조 인덱스 맵을 함께 관리합니다.</li>
 * </ul>
 *
 * @author 김민형
 * @since 26. 6. 24.
 */
public class InMemoryWatchingSessionRepository implements WatchingSessionRepository {

  // 사용자 아이디, 시청 세션
  Map<UUID, WatchingSession> watchingSessionStorage = new ConcurrentHashMap<>();
  // websocket session ID, watcher ID
  Map<String, UUID> sessionIdAndUserIdMap = new ConcurrentHashMap<>();

  private static final long EXPIRED_HOURS = 12;
  private static final long SCHEDULED_MINUTES = 30;

  @Override
  public WatchingSession save(WatchingSession watchingSession) {
    watchingSessionStorage.compute(watchingSession.getWatcherId(), (watcherId, previous) -> {
      if (previous != null) {
        sessionIdAndUserIdMap.remove(watchingSession.getSessionId());
      }
      sessionIdAndUserIdMap.put(watchingSession.getSessionId(), watchingSession.getWatcherId());
      return watchingSession;
    });
    return watchingSession;
  }

  @Override
  public Optional<WatchingSession> findByUserId(UUID userId) {
    return Optional.ofNullable(watchingSessionStorage.get(userId));
  }

  @Override
  public Optional<WatchingSession> findBySessionId(String sessionId) {
    UUID watcherId = sessionIdAndUserIdMap.get(sessionId);
    if (watcherId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(watchingSessionStorage.get(watcherId));
  }

  @Override
  public void deleteBySessionId(String sessionId) {

    UUID watcherId = sessionIdAndUserIdMap.remove(sessionId);

    if (watcherId != null) {
      watchingSessionStorage.computeIfPresent(watcherId, (k, session) -> {
        if (session.getSessionId().equals(sessionId)) {
          return null;
        }
        return session;
      });
    }
  }

  @Scheduled(fixedRate = SCHEDULED_MINUTES, timeUnit = TimeUnit.MINUTES)
  private void deleteExpiredSessions() {
    Instant expiredTime = Instant.now().minusSeconds(EXPIRED_HOURS * 3600);
    watchingSessionStorage.entrySet().removeIf(entry -> {
      WatchingSession watchingSession = entry.getValue();
      if (watchingSession.getCreatedAt().isBefore(expiredTime)) {
        sessionIdAndUserIdMap.remove(watchingSession.getSessionId());
        return true;
      }
      return false;
    });
  }

}
