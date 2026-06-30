package com.team6.moduply.conversation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team6.moduply.user.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "대화 정보")
public record ConversationDto(

    @Schema(description = "대화 ID", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id,

    @JsonProperty("with")
    @Schema(description = "대화 상대 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    UserSummaryDto withUser,

    @Schema(description = "마지막 메시지")
    DirectMessageDto lastestMessage,

    @Schema(description = "읽지 않은 메시지 존재 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean hasUnread
) {

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
}
