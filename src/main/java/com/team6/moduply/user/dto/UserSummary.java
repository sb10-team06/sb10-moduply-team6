package com.team6.moduply.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Objects;
import java.util.UUID;

public record UserSummary(

    @Schema(description = "사용자 ID", requiredMode = RequiredMode.REQUIRED)
    UUID userId,

    @Schema(description = "사용자 이름", requiredMode = RequiredMode.REQUIRED)
    String name,

    @Schema(description = "사용자 프로필 이미지 URL")
    String profileImageUrl
) {

  public UserSummary {
    Objects.requireNonNull(userId);
    Objects.requireNonNull(name);
  }

}
