package com.team6.moduply.directmessage.dto;

import com.team6.moduply.common.pagination.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Schema(description = "DM 목록 조회 요청")
public record DirectMessageFindAllRequest(
    @Schema(description = "커서")
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

  @AssertTrue(message = "cursor는 ISO-8601 형식이어야 합니다. (예: 2024-01-01T00:00:00Z)")
  @Schema(hidden = true)
  public boolean isCursorValid() {
    if (cursor == null) {
      return true;
    }

    try {
      Instant.parse(cursor);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
