package com.team6.moduply.directmessage.mapper;

import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.user.dto.UserSummaryDto;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class DirectMessageMapper {

  @Mapping(target = "id", source = "directMessage.id")
  @Mapping(target = "conversationId", source = "conversation.id")
  @Mapping(target = "createdAt", source = "directMessage.createdAt")
  @Mapping(target = "sender", expression = "java(resolveSender(directMessage, currentUserId, currentUserSummary, withUserSummary))")
  @Mapping(target = "receiver", expression = "java(resolveReceiver(directMessage, currentUserId, currentUserSummary, withUserSummary))")
  @Mapping(target = "content", source = "directMessage.content")
  public abstract DirectMessageDto toDto(
      DirectMessage directMessage,
      Conversation conversation,
      UUID currentUserId,
      UserSummaryDto currentUserSummary,
      UserSummaryDto withUserSummary
  );

  protected UserSummaryDto resolveSender(
      DirectMessage directMessage,
      UUID currentUserId,
      UserSummaryDto currentUserSummary,
      UserSummaryDto withUserSummary
  ) {
    return directMessage.getSender().getId().equals(currentUserId)
        ? currentUserSummary
        : withUserSummary;
  }

  protected UserSummaryDto resolveReceiver(
      DirectMessage directMessage,
      UUID currentUserId,
      UserSummaryDto currentUserSummary,
      UserSummaryDto withUserSummary
  ) {
    return directMessage.getSender().getId().equals(currentUserId)
        ? withUserSummary
        : currentUserSummary;
  }
}
