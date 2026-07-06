package com.team6.moduply.watching.command;

import java.util.Objects;
import java.util.UUID;

public record CreateWatchingSessionCommand(
    String sessionId,
    UUID userId,
    UUID contentId
) {

  public CreateWatchingSessionCommand {
    Objects.requireNonNull(sessionId);
    Objects.requireNonNull(userId);
    Objects.requireNonNull(contentId);
  }

}
