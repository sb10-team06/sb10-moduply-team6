package com.team6.moduply.content.dto;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "콘텐츠 목록 조회 요청")
public record ContentFindAllRequest(
    @Schema(description = "콘텐츠 타입", example = "movie")
    ContentType typeEqual,

    @Schema(description = "제목 또는 설명 검색어", example = "dream")
    String keywordLike,

    @Schema(description = "포함할 태그 목록", example = "[\"SF\", \"액션\"]")
    List<String> tagsIn,

    @Schema(description = "다음 페이지 커서")
    String cursor,

    @Schema(description = "다음 페이지 보조 커서")
    UUID idAfter,

    @Schema(description = "페이지 크기", example = "20")
    Integer limit,

    @Schema(description = "정렬 기준", example = "createdAt")
    ContentSortBy sortBy,

    @Schema(description = "정렬 방향", example = "DESCENDING")
    SortDirection sortDirection
) {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  public ContentFindAllRequest {
    limit = resolveLimit(limit);
    sortBy = sortBy == null ? ContentSortBy.createdAt : sortBy;
    sortDirection = sortDirection == null ? SortDirection.DESCENDING : sortDirection;
  }

  private static int resolveLimit(Integer limit) {
    if (limit == null || limit <= 0) {
      return DEFAULT_LIMIT;
    }

    return Math.min(limit, MAX_LIMIT);
  }
}
