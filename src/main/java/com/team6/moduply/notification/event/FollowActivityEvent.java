package com.team6.moduply.notification.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FollowActivityEvent {

  private final UUID actorId;
  private final String actorName;
  private final String activityContent;
}
