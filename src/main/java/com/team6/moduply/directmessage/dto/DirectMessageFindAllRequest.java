package com.team6.moduply.directmessage.dto;

import com.team6.moduply.common.pagination.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Schema(description = "DM 목록 조회 요청")
public record DirectMessageFindAllRequest(
    @Schema(description = "커서")
    @Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$",
        message = "cursor는 ISO-8601 형식이어야 합니다. (예: 2024-01-01T00:00:00Z)"
    )
    String cursor,

    @Schema(description = "보조 커서")
    UUID idAfter,

    @NotNull
    @Positive
    @Max(100)
    @Schema(description = "한 번에 가져올 개수")
    Integer limit,

    @NotNull
    @Schema(description = "정렬 방향")
    SortDirection sortDirection,

    @NotNull
    @Schema(description = "정렬 기준")
    DirectMessageSortBy sortBy
) {

  @AssertTrue(message = "cursor와 idAfter는 함께 전달해야 합니다.")
  @Schema(hidden = true)
  public boolean isCursorPairValid() {
    return (cursor == null && idAfter == null) || (cursor != null && idAfter != null);
  }
}
