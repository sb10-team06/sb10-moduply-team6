package com.team6.moduply.watching.dto;

import com.team6.moduply.user.dto.UserSummary;
import java.util.Objects;

public record ContentChatDto(
    UserSummary sender,
    String content
) {

  public ContentChatDto {
    Objects.requireNonNull(sender);
    Objects.requireNonNull(content);
  }

}
