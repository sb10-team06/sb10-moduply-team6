package com.team6.moduply.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "대화 생성 요청")
public record ConversationCreateRequest(

    @Schema(description = "대화 상대 사용자 ID", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "대화 상대 사용자 ID는 필수입니다.")
    UUID withUserId
) {
}
