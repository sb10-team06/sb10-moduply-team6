package com.team6.moduply.watching.dto;

import jakarta.validation.constraints.NotBlank;

public record ContentChatSendRequest(
    @NotBlank(message = "채팅 메세지는 비어있을 수 없습니다.")
    String content
) {

}
