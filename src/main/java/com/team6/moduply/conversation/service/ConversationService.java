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

  @Transactional(readOnly = true)
  public ConversationDto create(ConversationCreateRequest request, UUID currentUserId) {
    UUID withUserId = request.withUserId();
    log.debug("대화 생성 처리 시작. currentUserId={}, withUserId={}", currentUserId, withUserId);

    // 자기자신의 대화방 생성x
    if (currentUserId.equals(withUserId)) {
      throw new ConversationException(
          ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED,
          Map.of("userId", currentUserId)
      );
    }

    User currentUser = findUser(currentUserId);
    User withUser = findUser(withUserId);

    Conversation sortedConversation = Conversation.create(currentUserId, withUserId);
    Conversation conversation = conversationRepository
            .findByUser1IdAndUser2Id(
                    sortedConversation.getUser1Id(),
                    sortedConversation.getUser2Id()
            )
            .orElseGet(() -> {
              try {
                return conversationRepository.saveAndFlush(sortedConversation);

                // 이 경로에서 발생 가능한 DataIntegrityViolationException은
                // UNIQUE(user1_id, user2_id) 충돌만 존재한다.
              } catch (DataIntegrityViolationException e) {
                return conversationRepository
                        .findByUser1IdAndUser2Id(
                                sortedConversation.getUser1Id(),
                                sortedConversation.getUser2Id()
                        )
                        .orElseThrow(() ->
                                new ConversationException(
                                        ConversationErrorCode.CONVERSATION_CREATE_CONFLICT,
                                        Map.of(
                                                "user1Id", sortedConversation.getUser1Id(),
                                                "user2Id", sortedConversation.getUser2Id()
                                        ),
                                        e
                                ));
              }
            });

    ConversationDto response = toDto(conversation, currentUser, withUser);
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

  private ConversationDto toDto(Conversation conversation, User currentUser, User withUser) {
    // 가장 최신 DM 조회
    // 대화방 생성직후 lastesMessage는 null
    DirectMessage lastestMessage = directMessageRepository
        .findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
        .orElse(null);

    // 상대가 보낸 메시지중 안본게 있나?
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
