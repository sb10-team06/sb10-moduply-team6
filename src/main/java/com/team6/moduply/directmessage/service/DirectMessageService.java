package com.team6.moduply.directmessage.service;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.directmessage.dto.DirectMessageCreateRequest;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.mapper.DirectMessageMapper;
import com.team6.moduply.directmessage.repository.DirectMessageRepository;
import com.team6.moduply.notification.event.DirectMessageReceivedEvent;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMessageService {

  private final ConversationRepository conversationRepository;
  private final DirectMessageRepository directMessageRepository;
  private final UserRepository userRepository;
  private final DirectMessageMapper directMessageMapper;
  private final BinaryContentService binaryContentService;
  private final UserMapper userMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public DirectMessageDto create(
      UUID conversationId,
      DirectMessageCreateRequest request,
      UUID currentUserId
  ) {
    log.debug(
        "DM 생성 처리 시작. conversationId={}, currentUserId={}",
        conversationId,
        currentUserId
    );
    // 대화방 조회 및 참여자 검증
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));
    validateParticipant(conversation, currentUserId);

    User currentUser = findUser(currentUserId);
    UUID withUserId = resolveWithUserId(conversation, currentUserId);
    User withUser = findUser(withUserId);
    UserSummaryDto currentUserSummary = toUserSummaryDto(currentUser);
    UserSummaryDto withUserSummary = toUserSummaryDto(withUser);
    DirectMessage directMessage = directMessageRepository.save(
        DirectMessage.create(conversation, currentUser, request.content())
    );
    // 메시지 수신 이벤트 발행
    eventPublisher.publishEvent(new DirectMessageReceivedEvent(
            withUserId,
            currentUserId,
            currentUser.getName(),
            conversationId,
            request.content()
    ));

    DirectMessageDto response = directMessageMapper.toDto(
        directMessage,
        conversation,
        currentUserId,
        currentUserSummary,
        withUserSummary
    );

    log.debug("DM 생성 처리 완료. directMessageId={}", response.id());
    return response;
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("userId", userId)
        ));
  }

  private UserSummaryDto toUserSummaryDto(User user) {
    return userMapper.toSummaryDto(user, binaryContentService.generateUrl(user.getProfileImg()));
  }

  private void validateParticipant(Conversation conversation, UUID userId) {
    if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
      throw new ConversationException(
          ConversationErrorCode.CONVERSATION_FORBIDDEN,
          Map.of("conversationId", conversation.getId(), "userId", userId)
      );
    }
  }

  private UUID resolveWithUserId(Conversation conversation, UUID currentUserId) {
    if (conversation.getUser1Id().equals(currentUserId)) {
      return conversation.getUser2Id();
    }
    return conversation.getUser1Id();
  }
}
