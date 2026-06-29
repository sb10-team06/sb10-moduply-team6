package com.team6.moduply.content.dto;

import com.team6.moduply.content.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Objects;
import java.util.UUID;

public record ContentSummary(

    @Schema(description = "콘텐츠 ID", requiredMode = RequiredMode.REQUIRED)
    UUID id,

    @Schema(description = "콘텐츠 타입", requiredMode = RequiredMode.REQUIRED)
    ContentType type,

    @Schema(description = "콘텐츠 제목", requiredMode = RequiredMode.REQUIRED)
    String title,

    @Schema(description = "콘텐츠 설명")
    String description,

    @Schema(description = "썸네일 이미지 URL")
    String thumbnailUrl,

    @Schema(description = "콘텐츠 태그 목록", requiredMode = RequiredMode.REQUIRED)
    String[] tags,

    @Schema(description = "평균 평점", requiredMode = RequiredMode.REQUIRED)
    double averageRating,

    @Schema(description = "리뷰 개수", requiredMode = RequiredMode.REQUIRED)
    int reviewCount
) {

  public ContentSummary {
    Objects.requireNonNull(id);
    Objects.requireNonNull(type);
    Objects.requireNonNull(title);
    Objects.requireNonNull(tags);
  }
}
