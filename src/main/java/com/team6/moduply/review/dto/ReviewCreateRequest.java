package com.team6.moduply.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
    @NotNull
    @Schema(description = "콘텐츠 ID")
    java.util.UUID contentId,

    @NotBlank(message = "리뷰를 작성해주세요")
    @Schema(description = "리뷰 내용")
    String text,

    @NotNull
    @DecimalMin(value = "0.0", message = "평점은 0.0 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "평점은 5.0 이하이어야 합니다.")
    @Schema(description = "평점 (0.0 ~ 5.0)")
    Double rating
) {}
