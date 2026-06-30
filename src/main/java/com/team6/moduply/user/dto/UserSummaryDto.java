package com.team6.moduply.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "사용자 요약 정보")
public record UserSummaryDto(

    @Schema(description = "사용자 ID", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID userId,

    @Schema(description = "사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(description = "프로필 이미지 URL")
    String profileImageUrl
) {
}
