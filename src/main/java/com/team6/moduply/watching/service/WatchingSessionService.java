package com.team6.moduply.watching.service;

import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;

  public WatchingSession create(WatchingSession watchingSession) {
    return watchingSessionRepository.save(watchingSession);
  }

  public void delete(String sessionId) {
    watchingSessionRepository.deleteBySessionId(sessionId);
  }
}
