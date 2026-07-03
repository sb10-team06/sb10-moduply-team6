package com.team6.moduply.watching.dto;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.watching.enums.WatchingSessionSortBy;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)을 위한 조회 조건")
public record WatchingSessionQueryCondition(

    @Schema(description = "시청자 이름")
    String watcherNameLike,

    @Schema(description = "커서")
    String cursor,

    @Schema(description = "보조 커서(sessionId)")
    UUID idAfter,

    @Schema(description = "한 번에 가져올 개수", requiredMode = RequiredMode.REQUIRED)
    @Min(1)
    @Max(300)
    int limit,

    @Schema(description = "정렬방향", requiredMode = RequiredMode.REQUIRED)
    SortDirection sortDirection,

    @Schema(description = "정렬 기준", requiredMode = RequiredMode.REQUIRED)
    WatchingSessionSortBy sortBy
) {

  public WatchingSessionQueryCondition {
    sortDirection = Objects.requireNonNullElse(sortDirection, SortDirection.ASCENDING);
    sortBy = Objects.requireNonNullElse(sortBy, WatchingSessionSortBy.createdAt);
  }
}
