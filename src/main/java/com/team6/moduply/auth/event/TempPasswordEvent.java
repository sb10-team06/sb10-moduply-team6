package com.team6.moduply.auth.event;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TempPasswordEvent {
  private final String email;
  private final String tempPassword;
  private final Instant expiresAt;
}
