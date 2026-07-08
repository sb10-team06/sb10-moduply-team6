package com.team6.moduply.directmessage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DM 생성 요청")
public record DirectMessageCreateRequest(
    @NotBlank
    @Size(max = 1000)
    @Schema(description = "메시지 내용", requiredMode = Schema.RequiredMode.REQUIRED)
    String content
) {
}
