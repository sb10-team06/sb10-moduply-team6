package com.team6.moduply.watching.listener;

import com.team6.moduply.common.config.AsyncConfig;
import com.team6.moduply.watching.event.WatchingSessionChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "moduply.async.event.type",
    havingValue = "spring",
    matchIfMissing = true
)
public class SpringWatchingEventBroadcastListener {

  private final WatchingEventBroadcastProcessor processor;

  // spring 이벤트 리스너
  @ConditionalOnProperty(name = "moduply.async.event.type", havingValue = "spring", matchIfMissing = true)
  @Async(AsyncConfig.WATCHING_EVENT_TASK_EXECUTOR)
  @EventListener
  public void onSpringEvent(WatchingSessionChangedEvent event) {
    processor.on(event);
  }
}
