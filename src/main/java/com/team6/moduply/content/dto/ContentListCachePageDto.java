package com.team6.moduply.content.dto;

import com.team6.moduply.common.pagination.SortDirection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ContentListCachePageDto(
    List<ContentListCacheItemDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    SortDirection sortDirection
) {
  /// Redis 캐시에 저장될 data 리스트의 실제 구현체를 ArrayList로 고정하기 위한 방어 코드
  public ContentListCachePageDto {
    data = data == null ? List.of() : new ArrayList<>(data);
  }
}
