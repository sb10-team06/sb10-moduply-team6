package com.team6.moduply.directmessage.dto;

import java.time.Instant;
import java.util.UUID;

public record LatestDirectMessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    UUID senderId,
    String content
) {
}
