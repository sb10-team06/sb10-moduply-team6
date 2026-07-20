package com.team6.moduply.conversation.dto;

import com.team6.moduply.user.entity.User;
import java.time.Instant;
import java.util.UUID;

public record ConversationListItemDto(
    UUID id,
    Instant createdAt,
    User withUser,
    UUID lastMessageId,
    Instant lastMessageAt,
    String lastMessageContent,
    UUID lastMessageSenderId,
    long unreadCount
) {
}
