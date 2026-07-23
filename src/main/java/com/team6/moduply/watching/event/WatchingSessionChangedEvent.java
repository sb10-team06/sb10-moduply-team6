package com.team6.moduply.watching.event;

import com.team6.moduply.common.config.KafkaConfig;
import com.team6.moduply.common.kafka.ModuplyAsyncEvent;
import com.team6.moduply.watching.enums.ChangeType;
import com.team6.moduply.watching.model.WatchingSession;

public record WatchingSessionChangedEvent(
    ChangeType type,
    WatchingSession watchingSession,
    long watcherCount

) implements ModuplyAsyncEvent {

  @Override
  public String getTopic() {
    return KafkaConfig.WATCHING_SESSION_CHANGED_TOPIC;
  }

  @Override
  public String getPartitionKey() {
    return this.watchingSession.getId().toString();
  }
}
