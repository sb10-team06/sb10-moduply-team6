package com.team6.moduply.directmessage.dto;

import com.team6.moduply.user.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "다이렉트 메시지 정보")
public record DirectMessageDto(
    @Schema(description = "메시지 ID", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id,

    @Schema(description = "대화 ID", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID conversationId,

    @Schema(description = "메시지 생성 시각", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
    Instant createdAt,

    @Schema(description = "발신자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    UserSummaryDto sender,

    @Schema(description = "수신자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    UserSummaryDto receiver,

    @Schema(description = "메시지 내용")
    String content
) {
}
