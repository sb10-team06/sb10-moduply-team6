package com.team6.moduply.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ReviewDto (
    @Schema(description = "리뷰 ID")
    UUID id,

    @Schema(description = "콘텐츠 ID")
    UUID contentId,

    @Schema(description = "작성자 정보")
    AuthorDto author,

    @Schema(description = "리뷰 내용")
    String text,

    @Schema(description = "평점 (0.0 ~ 5.0)")
    Double rating
) {

  @Schema(description = "작성자 정보")
  public record AuthorDto(

      @Schema(description = "사용자 ID")
      UUID userId,

      @Schema(description = "사용자 이름")
      String name,

      @Schema(description = "사용자 프로필 이미지 URL")
      String profileImageUrl
  ) {
  }
}
