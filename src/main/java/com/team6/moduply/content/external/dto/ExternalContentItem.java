package com.team6.moduply.content.external.dto;

import com.team6.moduply.content.enums.ContentType;
import java.util.List;

public record ExternalContentItem(
    String externalApiId,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags
) {
}
