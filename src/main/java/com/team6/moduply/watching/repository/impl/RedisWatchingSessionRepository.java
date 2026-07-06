package com.team6.moduply.watching.repository.impl;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.watching.dto.WatchingSessionQueryCondition;
import com.team6.moduply.watching.enums.WatchingSessionSortBy;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.model.WatchingSessionResult;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
// TODO: [김민형] 향후 루아 스크립트 및 레디스 서치로 개선 예정
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
    UUID watcherId = watchingSession.getWatcher().userId();
    String watcherKey = WATCHER_KEY_PREFIX + watcherId.toString();
    String sessionKey = SESSION_KEY_PREFIX + watchingSession.getSessionId();
    String contentKey = CONTENT_KEY_PREFIX + watchingSession.getContentId().toString();

    WatchingSession previous = watchingSessionRedisTemplate.opsForValue().get(watcherKey);
    if (previous != null) {
      if (!previous.getSessionId().equals(watchingSession.getSessionId())) {
        sessionIdAndUserIdRedisTemplate.delete(SESSION_KEY_PREFIX + previous.getSessionId());
      }
      if (!previous.getContentId().equals(watchingSession.getContentId())) {
        String prevContentKey = CONTENT_KEY_PREFIX + previous.getContentId();
        sessionIdAndUserIdRedisTemplate.opsForSet().remove(prevContentKey, watcherId.toString());
        Long prevCount = countByContentId(previous.getContentId());
        if (prevCount == 0) {
          sessionIdAndUserIdRedisTemplate.delete(prevContentKey);
        }
      }
    }

    watchingSessionRedisTemplate.opsForValue()
        .set(watcherKey, watchingSession, EXPIRED_HOURS, TimeUnit.HOURS);
    sessionIdAndUserIdRedisTemplate.opsForValue()
        .set(sessionKey, watcherId.toString(), EXPIRED_HOURS, TimeUnit.HOURS);
    sessionIdAndUserIdRedisTemplate.opsForSet()
        .add(contentKey, watchingSession.getWatcher().userId().toString());

    sessionIdAndUserIdRedisTemplate.expire(contentKey, EXPIRED_HOURS, TimeUnit.HOURS);
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
          .remove(contentKey, session.getWatcher().userId().toString());

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

  @Override
  public SliceResult findAllByContentIdWithConditions(UUID contentId,
      WatchingSessionQueryCondition condition) {

    String contentKey = CONTENT_KEY_PREFIX + contentId.toString();

    Comparator<WatchingSession> comparator = getComparator(condition.sortBy(),
        condition.sortDirection());

    Set<String> watcherIds = sessionIdAndUserIdRedisTemplate.opsForSet().members(contentKey);

    if (watcherIds == null || watcherIds.isEmpty()) {
      return new SliceResult(List.of(), 0, false);
    }

    List<String> watcherKeys = watcherIds.stream()
        .map(watcherId -> WATCHER_KEY_PREFIX + watcherId)
        .toList();

    //multi Get으로 한 번에 조회
    List<WatchingSession> contentSessions = watchingSessionRedisTemplate.opsForValue()
        .multiGet(watcherKeys).stream()
        .filter(session -> session != null && contentId.equals(session.getContentId()))
        .toList();

    List<WatchingSession> filteredData = contentSessions.stream()
        .filter(s -> filterByWatcherNameLike(s, condition.watcherNameLike()))
        .toList();

    long totalCount = filteredData.size();

    List<WatchingSession> sortedData = filteredData.stream()
        .sorted(comparator)
        .filter(
            s -> filterAfterCursor(s, condition.cursor(), condition.idAfter(), condition.sortBy(),
                condition.sortDirection()))
        .limit(condition.limit() + 1)
        .toList();

    boolean hasNext = sortedData.size() > condition.limit();

    return new SliceResult(
        hasNext ? sortedData.subList(0, condition.limit()) : sortedData,
        totalCount,
        hasNext
    );
  }

  private boolean filterByWatcherNameLike(WatchingSession watchingSession, String watcherNameLike) {
    if (watcherNameLike == null || watcherNameLike.isBlank()) {
      return true;
    }
    return watchingSession.getWatcher().name().toLowerCase()
        .contains(watcherNameLike.toLowerCase());
  }

  private boolean filterAfterCursor(WatchingSession watchingSession, String cursor, UUID idAfter,
      WatchingSessionSortBy sortBy, SortDirection sortDirection) {
    if (cursor == null || cursor.isBlank()) {
      return true;
    }
    switch (sortBy) {
      case createdAt:
        Instant cursorTime = Instant.parse(cursor);
        Instant targetTime = watchingSession.getCreatedAt();
        if (sortDirection == SortDirection.ASCENDING) {
          if (targetTime.isAfter(cursorTime)) {
            return true;
          }
          if (targetTime.equals(cursorTime) && idAfter != null) {
            return watchingSession.getId().compareTo(idAfter) > 0;
          }
        } else {
          if (targetTime.isBefore(cursorTime)) {
            return true;
          }
          if (targetTime.equals(cursorTime) && idAfter != null) {
            return watchingSession.getId().compareTo(idAfter) < 0;
          }
        }
        return false;
    }
    return true;
  }

  private Comparator<WatchingSession> getComparator(WatchingSessionSortBy sortBy,
      SortDirection sortDirection) {
    Comparator<WatchingSession> comparator = switch (sortBy) {
      case createdAt -> Comparator.comparing(WatchingSession::getCreatedAt);
    };
    if (sortDirection == SortDirection.DESCENDING) {
      comparator = comparator.reversed();
    }
    return comparator.thenComparing(WatchingSession::getId);
  }

}
