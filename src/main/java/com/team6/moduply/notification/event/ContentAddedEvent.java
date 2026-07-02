package com.team6.moduply.notification.event;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ContentAddedEvent {
  private final UUID playlistId;
  private final String playlistTitle;
  private final String contentTitle;
  private final List<UUID> subscriberIds;
}
