package com.team6.moduply.watching.repository.impl;

import com.google.common.util.concurrent.Striped;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.model.WatchingSessionResult;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
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
  private final Map<UUID, WatchingSession> watchingSessionStorage = new ConcurrentHashMap<>();
  // websocket session ID, watcher ID
  private final Map<String, UUID> sessionIdAndUserIdMap = new ConcurrentHashMap<>();

  private final Striped<Lock> locks = Striped.lock(1024);

  private static final long EXPIRED_HOURS = 12;
  private static final long SCHEDULED_MINUTES = 30;

  @Override
  public WatchingSessionResult save(WatchingSession watchingSession) {
    //contentId로 락 걸기
    Lock lock = locks.get(watchingSession.getContentId());
    lock.lock();

    try {
      watchingSessionStorage.compute(watchingSession.getWatcherId(), (watcherId, previous) -> {
        if (previous != null) {
          sessionIdAndUserIdMap.remove(previous.getSessionId());
        }
        sessionIdAndUserIdMap.put(watchingSession.getSessionId(), watchingSession.getWatcherId());
        return watchingSession;
      });
      return new WatchingSessionResult(watchingSession,
          countByContentId(watchingSession.getContentId()));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<WatchingSession> findByUserId(UUID userId) {
    return Optional.ofNullable(watchingSessionStorage.get(userId));
  }

  @Override
  public Optional<WatchingSession> findBySessionId(String sessionId) {
    return Optional.ofNullable(sessionIdAndUserIdMap.get(sessionId))
        .map(watchingSessionStorage::get)
        .filter(session -> sessionId.equals(session.getSessionId()));
  }

  @Override
  public Optional<WatchingSessionResult> deleteBySessionId(String sessionId) {

    UUID watcherId = sessionIdAndUserIdMap.get(sessionId);

    if (watcherId == null) {
      return Optional.empty();
    }

    WatchingSession target = watchingSessionStorage.get(watcherId);
    if (target == null || !target.getSessionId().equals(sessionId)) {
      return Optional.empty();
    }

    Lock lock = locks.get(target.getContentId());
    lock.lock();

    try {
      sessionIdAndUserIdMap.remove(sessionId);
      WatchingSession[] removed = new WatchingSession[1];
      watchingSessionStorage.computeIfPresent(watcherId, (k, session) -> {
        if (session.getSessionId().equals(sessionId)) {
          removed[0] = session;
          return null;
        }
        return session;
      });
      if (removed[0] == null) {
        return Optional.empty();
      }
      return Optional.of(
          new WatchingSessionResult(removed[0], countByContentId(removed[0].getContentId())));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long countByContentId(UUID contentId) {
    return watchingSessionStorage.values().stream()
        .filter(s -> contentId.equals(s.getContentId()))
        .count();
  }

  @Scheduled(fixedRate = SCHEDULED_MINUTES, timeUnit = TimeUnit.MINUTES)
  private void deleteExpiredSessions() {
    Instant expiredTime = Instant.now().minusSeconds(EXPIRED_HOURS * 3600);
    watchingSessionStorage.entrySet().removeIf(entry -> {
      WatchingSession watchingSession = entry.getValue();
      if (watchingSession.getCreatedAt().isBefore(expiredTime)) {
        Lock lock = locks.get(watchingSession.getContentId());
        lock.lock();
        try {
          sessionIdAndUserIdMap.remove(watchingSession.getSessionId());
          return true;
        } finally {
          lock.unlock();
        }
      }
      return false;
    });
  }

}
