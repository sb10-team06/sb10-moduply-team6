package com.team6.moduply.auth.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EmailEvent {
  private final String email;
  private final String tempPassword;
}
