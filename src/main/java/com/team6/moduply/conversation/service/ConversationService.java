package com.team6.moduply.conversation.service;

import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.mapper.ConversationMapper;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.exception.DirectMessageErrorCode;
import com.team6.moduply.directmessage.exception.DirectMessageException;
import com.team6.moduply.directmessage.repository.DirectMessageRepository;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

  private final ConversationRepository conversationRepository;
  private final DirectMessageRepository directMessageRepository;
  private final UserRepository userRepository;
  private final ConversationMapper conversationMapper;

  @Transactional
  public ConversationDto create(ConversationCreateRequest request, UUID currentUserId) {
    UUID withUserId = request.withUserId();
    log.debug("대화 생성 처리 시작. currentUserId={}, withUserId={}", currentUserId, withUserId);

    if (currentUserId.equals(withUserId)) {
      throw new ConversationException(
          ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED,
          Map.of("userId", currentUserId)
      );
    }

    User currentUser = findUser(currentUserId);
    User withUser = findUser(withUserId);
    Conversation sortedConversation = Conversation.create(currentUserId, withUserId);

    if (conversationRepository.findByUser1IdAndUser2Id(
        sortedConversation.getUser1Id(),
        sortedConversation.getUser2Id()
    ).isPresent()) {
      throw new ConversationException(
          ConversationErrorCode.CONVERSATION_ALREADY_EXISTS,
          Map.of(
              "user1Id", sortedConversation.getUser1Id(),
              "user2Id", sortedConversation.getUser2Id()
          )
      );
    }

    Conversation conversation;
    try {
      conversation = conversationRepository.saveAndFlush(sortedConversation);
    } catch (DataIntegrityViolationException e) {
      throw new ConversationException(
          ConversationErrorCode.CONVERSATION_ALREADY_EXISTS,
          Map.of(
              "user1Id", sortedConversation.getUser1Id(),
              "user2Id", sortedConversation.getUser2Id()
          ),
          e
      );
    }

    ConversationDto response = conversationMapper.toCreateDto(conversation, currentUser, withUser);
    log.debug("대화 생성 처리 완료. conversationId={}", response.id());
    return response;
  }

  @Transactional(readOnly = true)
  public ConversationDto findById(UUID conversationId, UUID currentUserId) {
    log.debug("대화 조회 처리 시작. conversationId={}, currentUserId={}", conversationId, currentUserId);

    // 대화방 조회
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));
    // 참여중인 대화방 맞는지 검증.
    validateParticipant(conversation, currentUserId);

    ConversationDto response = toDto(conversation, currentUserId);
    log.debug("대화 조회 처리 완료. conversationId={}", response.id());
    return response;
  }

  @Transactional(readOnly = true)
  public ConversationDto findByUserId(UUID userId, UUID currentUserId) {
    log.debug("특정 사용자와의 대화 조회 처리 시작. currentUserId={}, userId={}", currentUserId, userId);

    if (currentUserId.equals(userId)) {
      throw new ConversationException(
          ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED,
          Map.of("userId", currentUserId)
      );
    }

    User currentUser = findUser(currentUserId);
    User withUser = findUser(userId);

    Conversation sortedConversation = Conversation.create(currentUserId, userId);
    Conversation conversation = conversationRepository.findByUser1IdAndUser2Id(
        sortedConversation.getUser1Id(),
        sortedConversation.getUser2Id()
    ).orElseThrow(() -> new ConversationException(
        ConversationErrorCode.CONVERSATION_NOT_FOUND,
        Map.of("userId", userId)
    ));

    ConversationDto response = toDto(conversation, currentUser, withUser);
    log.debug("특정 사용자와의 대화 조회 처리 완료. conversationId={}", response.id());
    return response;
  }

  @Transactional
  public void read(UUID conversationId, UUID directMessageId, UUID currentUserId) {
    log.debug(
        "DM 읽음 처리 시작. conversationId={}, directMessageId={}, currentUserId={}",
        conversationId,
        directMessageId,
        currentUserId
    );
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));
    validateParticipant(conversation, currentUserId);

    DirectMessage directMessage = directMessageRepository
        .findByIdAndConversationId(directMessageId, conversationId)
        .orElseThrow(() -> new DirectMessageException(
            DirectMessageErrorCode.DIRECT_MESSAGE_NOT_FOUND,
            Map.of("directMessageId", directMessageId, "conversationId", conversationId)
        ));

    // 자기자신 메세지 읽음처리 x
    if (directMessage.getSender().getId().equals(currentUserId)) {
      throw new DirectMessageException(
          DirectMessageErrorCode.DIRECT_MESSAGE_FORBIDDEN,
          Map.of("directMessageId", directMessageId, "userId", currentUserId)
      );
    }

    directMessage.markAsRead();
    log.debug("DM 읽음 처리 완료. directMessageId={}", directMessageId);
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("userId", userId)
        ));
  }

  private void validateParticipant(Conversation conversation, UUID userId) {
    if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
      throw new ConversationException(
          ConversationErrorCode.CONVERSATION_FORBIDDEN,
          Map.of("conversationId", conversation.getId(), "userId", userId)
      );
    }
  }

  /// 대화 상대방ID 조회
  private UUID resolveWithUserId(Conversation conversation, UUID currentUserId) {
    if (conversation.getUser1Id().equals(currentUserId)) {
      return conversation.getUser2Id();
    }
    return conversation.getUser1Id();
  }

  private ConversationDto toDto(Conversation conversation, UUID currentUserId) {
    User currentUser = findUser(currentUserId);
    User withUser = findUser(resolveWithUserId(conversation, currentUserId));

    return toDto(conversation, currentUser, withUser);
  }

  private ConversationDto toDto(Conversation conversation, User currentUser, User withUser) {
    DirectMessage lastestMessage = directMessageRepository
        .findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
        .orElse(null);
    boolean hasUnread = directMessageRepository
        .existsByConversationIdAndSenderIdNotAndReadFalse(conversation.getId(), currentUser.getId());

    return conversationMapper.toDto(
        conversation,
        currentUser,
        withUser,
        lastestMessage,
        hasUnread
    );
  }
}
