package com.team6.moduply.review.dto;

import com.team6.moduply.common.pagination.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @Schema(description = "한 번에 가져올 개수")
    Integer limit,

    @NotNull
    @Schema(description = "정렬 방향")
    SortDirection sortDirection,

    @NotNull
    @Schema(description = "정렬 기준")
    ReviewSortBy sortBy
) {}
