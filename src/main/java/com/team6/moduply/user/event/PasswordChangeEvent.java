package com.team6.moduply.user.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PasswordChangeEvent {
  private final String email;
}
