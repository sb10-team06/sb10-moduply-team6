package com.team6.moduply.content.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "콘텐츠 정렬 기준")
public enum ContentSortBy {
  @Schema(description = "최신순")
  createdAt("createdAt"),

  @Schema(description = "인기순")
  watcherCount("watcherCount"),

  @Schema(description = "평점순")
  rate("averageRating");

  private final String propertyName;

  ContentSortBy(String propertyName) {
    this.propertyName = propertyName;
  }

  public String propertyName() {
    return propertyName;
  }
}
