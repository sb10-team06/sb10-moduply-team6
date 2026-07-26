package com.team6.moduply.watching.listener;

import com.team6.moduply.common.redis.RedisPublisher;
import com.team6.moduply.content.dto.ContentSummary;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.service.ContentService;
import com.team6.moduply.watching.event.WatchingSessionChangedEvent;
import com.team6.moduply.watching.dto.WatchingSessionChange;
import com.team6.moduply.watching.dto.WatchingSessionDto;
import com.team6.moduply.watching.util.mapper.WatchingSessionMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingEventBroadcastProcessor {

  private final ContentService contentService;
  private final WatchingSessionMapper watchingSessionMapper;
  private final RedisPublisher redisPublisher;

  public void on(WatchingSessionChangedEvent event) {

    ContentSummary content;
    try {
      content = ContentSummary.from(contentService.find(event.watchingSession().getContentId()));
    } catch (ContentException e) {
      log.info("존재하지 않거나 삭제된 컨텐츠: 변경 메세지를 보내지 않습니다: contentId={}",
          event.watchingSession().getContentId(), e);
      return;
    }

    UUID contentId = content.id();
    WatchingSessionDto sessionDto = watchingSessionMapper.toDto(event.watchingSession(),
        content);

    WatchingSessionChange message = new WatchingSessionChange(
        event.type(),
        sessionDto,
        event.watcherCount()
    );
    String destination = String.format("/sub/contents/%s/watch",
        contentId.toString());
    redisPublisher.publish(destination, message);
    log.info("시청 세션 변경 메세지 전송 완료: contentId={}", contentId);
  }
}
