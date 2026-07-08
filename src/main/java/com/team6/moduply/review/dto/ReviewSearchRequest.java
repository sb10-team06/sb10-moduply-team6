package com.team6.moduply.review.dto;

import com.team6.moduply.common.pagination.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public record ReviewSearchRequest(

    @NotNull
    @Schema(description = "콘텐츠 ID")
    UUID contentIdEqual,

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
    ReviewSortBy sortBy
) {
    @AssertTrue(message = "cursor와 idAfter는 함께 제공되어야 합니다.")
    public boolean isCursorAndIdAfterBothPresent() {
        return (cursor == null) == (idAfter == null);
    }

    @AssertTrue(message = "rating 정렬 시 cursor는 'rating:createdAt' 형식이어야 합니다.")
    public boolean isRatingCursorValid() {
        if (sortBy != ReviewSortBy.rating || cursor == null) return true;
        try {
            int colonIndex = cursor.indexOf(":");
            if (colonIndex <= 0) return false;
            Double.parseDouble(cursor.substring(0, colonIndex));
            Instant.parse(cursor.substring(colonIndex + 1));
            return true;
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    @AssertTrue(message = "createdAt 정렬 시 cursor는 ISO-8601 형식이어야 합니다.")
    public boolean isCreatedAtCursorValid() {
        if (sortBy != ReviewSortBy.createdAt || cursor == null) return true;
        try {
            Instant.parse(cursor);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

}
