package com.team6.moduply.watching.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class WatchingSession {

  private final UUID id;
  private final String sessionId; // websocket session ID
  private final UUID watcherId;
  private final UUID contentId;
  private final Instant createdAt;

  private WatchingSession(String sessionId, UUID watcherId, UUID contentId) {
    this.id = UUID.randomUUID();
    this.sessionId = sessionId;
    this.watcherId = watcherId;
    this.contentId = contentId;
    this.createdAt = Instant.now();
  }

  @JsonCreator
  private WatchingSession(
      @JsonProperty("id") UUID id,
      @JsonProperty("sessionId") String sessionId,
      @JsonProperty("watcherId") UUID watcherId,
      @JsonProperty("contentId") UUID contentId,
      @JsonProperty("createdAt") Instant createdAt
  ) {
    this.id = id;
    this.sessionId = sessionId;
    this.watcherId = watcherId;
    this.contentId = contentId;
    this.createdAt = createdAt;
  }

  public static WatchingSession create(String sessionId, UUID watcherId, UUID contentId) {
    return new WatchingSession(sessionId, watcherId, contentId);
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
