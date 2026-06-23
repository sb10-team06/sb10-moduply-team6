package com.team6.moduply.content.dto;

import com.team6.moduply.content.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "콘텐츠 응답")
public record ContentDto(
    @Schema(description = "콘텐츠 ID")
    UUID id,

    @Schema(description = "콘텐츠 타입")
    ContentType type,

    @Schema(description = "콘텐츠 제목")
    String title,

    @Schema(description = "콘텐츠 설명")
    String description,

    @Schema(description = "썸네일 이미지 URL")
    String thumbnailUrl,

    @Schema(description = "콘텐츠 태그 목록")
    List<String> tags,

    @Schema(description = "평균 평점")
    BigDecimal averageRating,

    @Schema(description = "리뷰 개수")
    int reviewCount,

    @Schema(description = "시청자 수")
    long watcherCount
) {
}
