package com.team6.moduply.content.dto;

import com.team6.moduply.common.pagination.SortDirection;
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
}
