package com.team6.moduply.watching.service;

import com.team6.moduply.watching.event.WatchingSessionChangedEvent;
import com.team6.moduply.watching.enums.ChangeType;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.model.WatchingSessionResult;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  public WatchingSession create(WatchingSession watchingSession) {
    WatchingSessionResult savedWatchingSession = watchingSessionRepository.save(watchingSession);
    applicationEventPublisher.publishEvent(
        new WatchingSessionChangedEvent(ChangeType.JOIN, savedWatchingSession.watchingSession(),
            savedWatchingSession.watcherCount()));
    return savedWatchingSession.watchingSession();
  }

  public void delete(String sessionId) {
    watchingSessionRepository.deleteBySessionId(sessionId)
        .ifPresent(deletedWatchingSession -> applicationEventPublisher.publishEvent(
            new WatchingSessionChangedEvent(ChangeType.LEAVE,
                deletedWatchingSession.watchingSession(), deletedWatchingSession.watcherCount())));
  }

  public long countByContentId(UUID contentId) {
    return watchingSessionRepository.countByContentId(contentId);
  }
}
