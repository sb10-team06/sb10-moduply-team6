package com.team6.moduply.notification.event;

import com.team6.moduply.user.enums.Role;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserRoleUpdatedEvent {
  private final UUID receiverId;
  private final Role oldRole;
  private final Role newRole;
}
