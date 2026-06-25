package com.team6.moduply.playlist.dto;

import com.team6.moduply.common.pagination.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record PlaylistSearchRequest (
    @Schema(description = "검색 키워드")
    String keywordLike,

    @Schema(description = "소유자 ID")
    UUID ownerIdEqual,

    @Schema(description = "구독자 ID")
    UUID subscriberIdEqual,

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
    String sortBy
) {}
