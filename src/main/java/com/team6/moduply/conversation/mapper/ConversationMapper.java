package com.team6.moduply.conversation.mapper;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.mapper.UserMapper;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public abstract class ConversationMapper {

  @Autowired
  protected BinaryContentService binaryContentService;

  @Autowired
  protected UserMapper userMapper;

  @Mapping(target = "id", source = "conversation.id")
  @Mapping(target = "withUser", expression = "java(toUserSummaryDto(withUser))")
  @Mapping(target = "lastestMessage", expression = "java(toDirectMessageDto(lastestMessage, conversation, currentUser, withUser))")
  @Mapping(target = "hasUnread", source = "hasUnread")
  public abstract ConversationDto toDto(
      Conversation conversation,
      User currentUser,
      User withUser,
      DirectMessage lastestMessage,
      boolean hasUnread
  );

  @Mapping(target = "id", source = "conversation.id")
  @Mapping(target = "withUser", expression = "java(toUserSummaryDto(withUser))")
  @Mapping(target = "lastestMessage", ignore = true)
  @Mapping(target = "hasUnread", constant = "false")
  public abstract ConversationDto toCreateDto(
      Conversation conversation,
      User currentUser,
      User withUser
  );

  protected ConversationDto.DirectMessageDto toDirectMessageDto(
      DirectMessage message,
      Conversation conversation,
      User currentUser,
      User withUser
  ) {
    if (message == null) {
      return null;
    }

    User sender = message.getSender();
    User receiver = resolveReceiver(sender.getId(), currentUser, withUser);
    return new ConversationDto.DirectMessageDto(
        message.getId(),
        conversation.getId(),
        message.getCreatedAt(),
        toUserSummaryDto(sender),
        toUserSummaryDto(receiver),
        message.getContent()
    );
  }

  protected UserSummaryDto toUserSummaryDto(User user) {
    return userMapper.toSummaryDto(user, binaryContentService.generateUrl(user.getProfileImg()));
  }

  private User resolveReceiver(UUID senderId, User currentUser, User withUser) {
    return currentUser.getId().equals(senderId) ? withUser : currentUser;
  }
}
