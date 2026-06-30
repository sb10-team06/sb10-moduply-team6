package com.team6.moduply.conversation.service;

import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.mapper.ConversationMapper;
import com.team6.moduply.conversation.repository.ConversationRepository;
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
    // 대화방이 이미 존재하면
    ).isPresent()) {
      throw new ConversationException(
              ConversationErrorCode.CONVERSATION_ALREADY_EXISTS,
              Map.of("user1Id", sortedConversation.getUser1Id(),
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
              Map.of("user1Id", sortedConversation.getUser1Id(),
                      "user2Id", sortedConversation.getUser2Id()
              ),
              e
      );
    }

    ConversationDto response = conversationMapper.toCreateDto(conversation, currentUser, withUser);
    log.debug("대화 생성 처리 완료. conversationId={}", response.id());
    return response;
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("userId", userId)
        ));
  }

}
