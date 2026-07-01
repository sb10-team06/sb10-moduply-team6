package com.team6.moduply.conversation.service;

import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.mapper.ConversationMapper;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.directmessage.entity.DirectMessage;
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

    // 나와 상대방 객체 생성
    User currentUser = findUser(currentUserId);
    User withUser = findUser(resolveWithUserId(conversation, currentUserId));

    // 대화방의 가장 최근 메시지 조회
    // 메시지 없으면 null
    DirectMessage lastestMessage = directMessageRepository
        .findTopByConversationIdOrderByCreatedAtDesc(conversationId)
        .orElse(null);
    // 상대방이 보낸 메시지중 안읽은게 있는지?
    boolean hasUnread = directMessageRepository
        .existsByConversationIdAndSenderIdNotAndReadFalse(conversationId, currentUserId);

    ConversationDto response = conversationMapper.toDto(
        conversation,
        currentUser,
        withUser,
        lastestMessage,
        hasUnread
    );
    log.debug("대화 조회 처리 완료. conversationId={}", response.id());
    return response;
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("userId", userId)
        ));
  }

  private void validateParticipant(Conversation conversation, UUID userId) {
    // 자기가 참여하고 있는 대화방만 조회할 수 있다.
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
}
