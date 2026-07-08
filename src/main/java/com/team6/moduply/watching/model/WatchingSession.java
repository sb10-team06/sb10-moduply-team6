package com.team6.moduply.watching.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team6.moduply.user.dto.UserSummary;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class WatchingSession {

  private final UUID id;
  private final String sessionId; // websocket session ID
  private final UserSummary watcher;
  private final UUID contentId;
  private final Instant createdAt;

  private WatchingSession(String sessionId, UserSummary watcher, UUID contentId) {
    this.id = UUID.randomUUID();
    this.sessionId = sessionId;
    this.watcher = watcher;
    this.contentId = contentId;
    this.createdAt = Instant.now();
  }

  @JsonCreator
  private WatchingSession(
      @JsonProperty("id") UUID id,
      @JsonProperty("sessionId") String sessionId,
      @JsonProperty("watcher") UserSummary watcher,
      @JsonProperty("contentId") UUID contentId,
      @JsonProperty("createdAt") Instant createdAt
  ) {
    this.id = id;
    this.sessionId = sessionId;
    this.watcher = watcher;
    this.contentId = contentId;
    this.createdAt = createdAt;
  }

  public static WatchingSession create(String sessionId, UserSummary watcher, UUID contentId) {
    return new WatchingSession(sessionId, watcher, contentId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WatchingSession that = (WatchingSession) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
