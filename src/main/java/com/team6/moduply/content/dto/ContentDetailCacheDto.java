package com.team6.moduply.content.dto;

import com.team6.moduply.content.enums.ContentType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ContentDetailCacheDto(
    UUID id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    BigDecimal averageRating,
    int reviewCount
) {
  public ContentDetailCacheDto {
    tags = tags == null ? List.of() : new ArrayList<>(tags);
  }
}
