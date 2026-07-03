package com.team6.moduply.watching.service;

import com.team6.moduply.content.dto.ContentSummary;
import com.team6.moduply.content.service.ContentService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserSummary;
import com.team6.moduply.user.service.UserService;
import com.team6.moduply.watching.command.CreateWatchingSessionCommand;
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
  private final ContentService contentService;
  private final UserService userService;

  public WatchingSession create(CreateWatchingSessionCommand command) {
    contentService.validateExists(command.contentId());
    UserDto userDto = userService.getUser(command.userId());
    WatchingSession watchingSession = WatchingSession.create(
        command.sessionId(),
        UserSummary.from(userDto),
        command.contentId()
    );
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
    WatchingSession watchingSession = watchingSessionRepository.findByUserId(userId).orElse(null);
    if (watchingSession == null) {
      return null;
    }
    ContentSummary content = ContentSummary.from(
        contentService.find(watchingSession.getContentId()));
    return watchingSessionMapper.toDto(watchingSession, content);
  }
}
