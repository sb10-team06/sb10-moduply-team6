package com.team6.moduply.playlist.dto;

import com.team6.moduply.content.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(

    @Schema(description = "플레이리스트 ID")
    UUID id,

    @Schema(description = "플레이리스트 소유자")
    OwnerDto owner,

    @Schema(description = "플레이리스트 제목")
    String title,

    @Schema(description = "플레이리스트 설명")
    String description,

    @Schema(description = "수정 시각")
    Instant updatedAt,

    @Schema(description = "구독자 수")
    Long subscriberCount,

    @Schema(description = "구독 여부")
    boolean subscribedByMe,

    @Schema(description = "플레이리스트에 포함된 콘텐츠 목록")
    List<ContentSummaryDto> contents
) {
  @Schema(description = "플레이리스트 소유자 정보")
  public record OwnerDto(

      @Schema(description = "사용자 ID")
      UUID userId,

      @Schema(description = "사용자 이름")
      String name,

      @Schema(description = "사용자 프로필 이미지 URL")
      String profileImageUrl
  ) {}

  @Schema(description = "콘텐츠 요약 정보")
  public record ContentSummaryDto(

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
      Double averageRating,

      @Schema(description = "리뷰 개수")
      Integer reviewCount
  ) {}
}