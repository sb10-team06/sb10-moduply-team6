package com.team6.moduply.directmessage.mapper;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.mapper.UserMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DirectMessageMapper {

  private final BinaryContentService binaryContentService;
  private final UserMapper userMapper;

  public DirectMessageMapper(BinaryContentService binaryContentService, UserMapper userMapper) {
    this.binaryContentService = binaryContentService;
    this.userMapper = userMapper;
  }

  public DirectMessageDto toDto(
      DirectMessage directMessage,
      Conversation conversation,
      User currentUser,
      User withUser
  ) {
    User sender = directMessage.getSender();
    User receiver = resolveReceiver(sender.getId(), currentUser, withUser);

    return new DirectMessageDto(
        directMessage.getId(),
        conversation.getId(),
        directMessage.getCreatedAt(),
        toUserSummaryDto(sender),
        toUserSummaryDto(receiver),
        directMessage.getContent()
    );
  }

  private UserSummaryDto toUserSummaryDto(User user) {
    return userMapper.toSummaryDto(user, binaryContentService.generateUrl(user.getProfileImg()));
  }

  private User resolveReceiver(UUID senderId, User currentUser, User withUser) {
    return currentUser.getId().equals(senderId) ? withUser : currentUser;
  }
}
