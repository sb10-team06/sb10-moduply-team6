package com.team6.moduply.auth.event;

import java.util.Objects;
import java.util.UUID;

public record UserLogoutEvent(
    UUID userId,
    String email
) {

  public UserLogoutEvent {
    Objects.requireNonNull(userId);
    Objects.requireNonNull(email);
  }
}
