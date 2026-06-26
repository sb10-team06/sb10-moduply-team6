package com.team6.moduply.content.dto;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "콘텐츠 목록 조회 요청")
public record ContentFindAllRequest(
    @Schema(description = "콘텐츠 타입", example = "movie")
    ContentType typeEqual,

    @Schema(description = "제목 또는 설명 검색어", example = "dream")
    String keywordLike,

    @Schema(description = "포함할 태그 목록", example = "[\"SF\", \"액션\"]")
    List<@NotBlank(message = "태그 이름은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "태그 이름은 100자 이하여야 합니다.") String> tagsIn,

    @Schema(description = "다음 페이지 커서")
    String cursor,

    @Schema(description = "다음 페이지 보조 커서")
    UUID idAfter,

    @Schema(description = "페이지 크기", example = "20")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    Integer limit,

    @Schema(description = "정렬 기준", example = "createdAt")
    ContentSortBy sortBy,

    @Schema(description = "정렬 방향", example = "DESCENDING")
    SortDirection sortDirection
) {

  private static final int DEFAULT_LIMIT = 20;

  public ContentFindAllRequest {
    limit = limit == null ? DEFAULT_LIMIT : limit;
    sortBy = sortBy == null ? ContentSortBy.createdAt : sortBy;
    sortDirection = sortDirection == null ? SortDirection.DESCENDING : sortDirection;
  }

  @AssertTrue(message = "cursor와 idAfter는 함께 전달되어야 합니다.")
  @Schema(hidden = true)
  public boolean isCursorPairValid() {
    return (cursor == null && idAfter == null) || (cursor != null && idAfter != null);
  }
}
