package com.team6.moduply.content.dto;

import com.team6.moduply.content.enums.ContentType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ContentListCacheItemDto(
    UUID id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    BigDecimal averageRating,
    int reviewCount
) {
  /// Redis 캐시에 저장될 data 리스트의 실제 구현체를 ArrayList로 고정하기 위한 방어 코드
  public ContentListCacheItemDto {
    tags = tags == null ? List.of() : new ArrayList<>(tags);
  }
}
