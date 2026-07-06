package com.team6.moduply.watching.repository;

import com.team6.moduply.watching.dto.WatchingSessionQueryCondition;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.model.WatchingSessionResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRepository {

  WatchingSessionResult save(WatchingSession watchingSession);

  Optional<WatchingSession> findByUserId(UUID userId);

  Optional<WatchingSession> findBySessionId(String sessionId);

  Optional<WatchingSessionResult> deleteBySessionId(String sessionId);

  long countByContentId(UUID contentId);

  SliceResult findAllByContentIdWithConditions(UUID contentId,
      WatchingSessionQueryCondition condition);

  record SliceResult(
      List<WatchingSession> data,
      long totalCount,
      boolean hasNext
  ) {

  }
}
