package com.team6.moduply.notification.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PlaylistSubscribedEvent {
  private final UUID playlistOwnerId;
  private final UUID subscriberId;
  private final UUID playlistId;
  private final String playlistTitle;
}
