package com.team6.moduply.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정렬 방향")
public enum SortDirection {
  @Schema(description = "오름차순")
  ASCENDING,

  @Schema(description = "내림차순")
  DESCENDING
}
