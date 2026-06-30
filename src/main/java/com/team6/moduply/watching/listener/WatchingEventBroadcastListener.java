package com.team6.moduply.watching.listener;

import com.team6.moduply.common.config.AsyncConfig;
import com.team6.moduply.content.dto.ContentSummary;
import com.team6.moduply.watching.event.WatchingSessionChangedEvent;
import com.team6.moduply.content.service.ContentQueryService;
import com.team6.moduply.user.dto.UserSummary;
import com.team6.moduply.user.service.UserQueryService;
import com.team6.moduply.watching.dto.WatchingSessionChange;
import com.team6.moduply.watching.dto.WatchingSessionDto;
import com.team6.moduply.watching.enums.ChangeType;
import com.team6.moduply.watching.service.WatchingSessionService;
import com.team6.moduply.watching.util.mapper.WatchingSessionMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingEventBroadcastListener {

  private final UserQueryService userQueryService;
  private final ContentQueryService contentQueryService;
  private final WatchingSessionMapper watchingSessionMapper;
  private final SimpMessagingTemplate messagingTemplate;
  private final WatchingSessionService watchingSessionService;

  @Async(AsyncConfig.WATCHING_EVENT_TASK_EXECUTOR)
  @EventListener
  public void on(WatchingSessionChangedEvent event) {

    ContentSummary content;
    try {
      content = contentQueryService.find(event.watchingSession().getContentId());
    } catch (Exception e) {
      log.info("존재하지 않거나 삭제된 컨텐츠: 변경 메세지를 보내지 않습니다: contentId={}",
          event.watchingSession().getContentId(), e);
      return;
    }
    UserSummary watcher;
    UUID contentId = content.id();
    UUID watcherId = event.watchingSession().getWatcherId();
    if (event.type() == ChangeType.JOIN) {
      log.debug("시청 세션 JOIN: contentId={}, watcherId={}", contentId, watcherId);
      watcher = userQueryService.findById(watcherId);
    } else {
      log.debug("시청 세션 LEAVE: contentId={}, watcherId={}", contentId, watcherId);
      watcher = new UserSummary(contentId, "", null);
    }

    WatchingSessionDto sessionDto = watchingSessionMapper.toDto(event.watchingSession(), watcher,
        content);

    long watcherCount = watchingSessionService.countByContentId(content.id());
    WatchingSessionChange message = new WatchingSessionChange(
        event.type(),
        sessionDto,
        watcherCount
    );
    String destination = String.format("/sub/contents/%s/watch",
        contentId.toString());
    messagingTemplate.convertAndSend(destination, message);
    log.info("시청 세션 변경 메세지 전송 완료: contentId={}", contentId);
  }

}
