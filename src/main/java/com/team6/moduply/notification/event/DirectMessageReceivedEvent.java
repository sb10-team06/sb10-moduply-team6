package com.team6.moduply.notification.event;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DirectMessageReceivedEvent {

  private final UUID receiverId;
  private final UUID senderId;
  private final String senderName;
  private final UUID conversationId;
  private final String content;
}
