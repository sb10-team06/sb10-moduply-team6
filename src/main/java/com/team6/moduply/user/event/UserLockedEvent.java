package com.team6.moduply.user.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class UserLockedEvent {
  private final String email;
}
