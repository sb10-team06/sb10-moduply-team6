package com.team6.moduply.watching.service;

import com.team6.moduply.content.dto.ContentSummary;
import com.team6.moduply.content.service.ContentQueryService;
import com.team6.moduply.user.dto.UserSummary;
import com.team6.moduply.user.service.UserQueryService;
import com.team6.moduply.watching.dto.WatchingSessionDto;
import com.team6.moduply.watching.event.WatchingSessionChangedEvent;
import com.team6.moduply.watching.enums.ChangeType;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.model.WatchingSessionResult;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import com.team6.moduply.watching.util.mapper.WatchingSessionMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final WatchingSessionMapper watchingSessionMapper;
  private final UserQueryService userQueryService;
  private final ContentQueryService contentQueryService;

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

  public WatchingSessionDto findByUserId(UUID userId) {
    UserSummary watcher = userQueryService.findById(userId);
    WatchingSession watchingSession = watchingSessionRepository.findByUserId(userId).orElse(null);
    if (watchingSession == null) {
      return null;
    }
    ContentSummary content = contentQueryService.find(watchingSession.getContentId());
    return watchingSessionMapper.toDto(watchingSession, watcher, content);
  }
}
