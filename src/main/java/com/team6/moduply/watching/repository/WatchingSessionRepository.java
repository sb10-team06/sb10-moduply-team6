package com.team6.moduply.watching.repository;

import com.team6.moduply.watching.model.WatchingSession;
import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRepository {

  WatchingSession save(WatchingSession watchingSession);

  Optional<WatchingSession> findByUserId(UUID userId);

  Optional<WatchingSession> findBySessionId(String sessionId);

  void deleteBySessionId(String sessionId);
}
