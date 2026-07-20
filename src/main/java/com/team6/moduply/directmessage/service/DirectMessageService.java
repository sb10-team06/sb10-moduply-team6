package com.team6.moduply.directmessage.service;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.entity.ConversationUserState;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.conversation.repository.ConversationUserStateRepository;
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
import java.util.Optional;
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
  private final ConversationUserStateRepository conversationUserStateRepository;
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
    log.debug("DM 생성 처리를 시작합니다. conversationId={}, currentUserId={}", conversationId, currentUserId);

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
    /// DM 동시 생성시 conversation.lastMessage가 lost update 방지
    conversationRepository.updateLastMessageIfNewer(
        conversationId,
        directMessage.getId(),
        directMessage.getCreatedAt(),
        directMessage.getContent(),
        currentUserId
    );
    ConversationUserState receiverState = Optional.ofNullable(conversationUserStateRepository
        .findByConversationIdAndUserId(conversationId, withUserId))
        .flatMap(optionalState -> optionalState)
        .orElseGet(() -> createState(conversation, withUserId));
    receiverState.increaseUnreadCount();

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

    log.debug("DM 생성 처리를 완료했습니다. directMessageId={}", response.id());
    return response;
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("userId", userId)
        ));
  }

  private ConversationUserState createState(Conversation conversation, UUID userId) {
    ConversationUserState state = ConversationUserState.create(conversation, userId);
    ConversationUserState savedState = conversationUserStateRepository.save(state);
    return savedState != null ? savedState : state;
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
