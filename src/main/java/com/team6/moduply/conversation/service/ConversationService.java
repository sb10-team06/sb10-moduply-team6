package com.team6.moduply.conversation.service;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.dto.ConversationFindAllRequest;
import com.team6.moduply.conversation.dto.ConversationListItemDto;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.entity.ConversationUserState;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.mapper.ConversationMapper;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.conversation.repository.ConversationUserStateRepository;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.dto.DirectMessageFindAllRequest;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.exception.DirectMessageErrorCode;
import com.team6.moduply.directmessage.exception.DirectMessageException;
import com.team6.moduply.directmessage.mapper.DirectMessageMapper;
import com.team6.moduply.directmessage.repository.DirectMessageRepository;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final ConversationUserStateRepository conversationUserStateRepository;
  private final DirectMessageRepository directMessageRepository;
  private final UserRepository userRepository;
  private final ConversationMapper conversationMapper;
  private final DirectMessageMapper directMessageMapper;
  private final BinaryContentService binaryContentService;
  private final UserMapper userMapper;

  @Transactional
  public ConversationDto create(ConversationCreateRequest request, UUID currentUserId) {
    UUID withUserId = request.withUserId();
    log.debug("대화방 생성 처리를 시작합니다. currentUserId={}, withUserId={}", currentUserId, withUserId);

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
      conversationUserStateRepository.saveAll(List.of(
          ConversationUserState.create(conversation, currentUserId),
          ConversationUserState.create(conversation, withUserId)
      ));
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
    log.debug("대화방 생성 처리를 완료했습니다. conversationId={}", response.id());
    return response;
  }

  @Transactional(readOnly = true)
  public ConversationDto findById(UUID conversationId, UUID currentUserId) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));
    validateParticipant(conversation, currentUserId);

    return toDto(conversation, currentUserId);
  }

  @Transactional(readOnly = true)
  public ConversationDto findByUserId(UUID userId, UUID currentUserId) {
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

    return toDto(conversation, currentUser, withUser);
  }

  @Transactional(readOnly = true)
  public CursorResponse<ConversationDto> findAll(
      ConversationFindAllRequest request,
      UUID currentUserId
  ) {
    /// (1)현재 User 조회
    User currentUser = findUser(currentUserId);
    UserDto currentUserDto = new UserDto(
        currentUser.getId(),
        currentUser.getCreatedAt(),
        currentUser.getEmail(),
        currentUser.getName(),
        binaryContentService.generateUrl(currentUser.getProfileImg()),
        currentUser.getRole(),
        currentUser.isBlocked()
    );
    return findAll(request, currentUserDto);
  }

  @Transactional(readOnly = true)
  public CursorResponse<ConversationDto> findAll(
      ConversationFindAllRequest request,
      UserDto currentUser
  ) {
    UUID currentUserId = currentUser.getId();
    log.debug(
        "대화방 목록 조회 처리를 시작합니다. currentUserId={}, keywordLike={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
        currentUserId,
        request.keywordLike(),
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortBy(),
        request.sortDirection()
    );

    List<ConversationListItemDto> conversations =
        conversationRepository.findAllDtoWithCursor(request, currentUserId);
    long totalCount = conversationRepository.countWithCondition(request, currentUserId);
    boolean hasNext = conversations.size() > request.limit();

    if (hasNext) {
      conversations = conversations.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext) {
      ConversationListItemDto last = conversations.get(conversations.size() - 1);
      nextCursor = last.createdAt().toString();
      nextIdAfter = last.id();
    }

    UserSummaryDto currentUserSummary = toUserSummaryDto(currentUser);
    List<ConversationDto> data = conversations.stream()
        .map(conversation -> toConversationDto(conversation, currentUserId, currentUserSummary))
        .toList();

    CursorResponse<ConversationDto> response = new CursorResponse<>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );
    log.debug("대화방 목록 조회 처리를 완료했습니다. count={}, hasNext={}", data.size(), hasNext);
    return response;
  }

  @Transactional
  public void read(UUID conversationId, UUID directMessageId, UUID currentUserId) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));
    validateParticipant(conversation, currentUserId);

    DirectMessage readUntilMessage = directMessageRepository
        .findByIdAndConversationId(directMessageId, conversationId)
        .orElseThrow(() -> new DirectMessageException(
            DirectMessageErrorCode.DIRECT_MESSAGE_NOT_FOUND,
            Map.of("directMessageId", directMessageId, "conversationId", conversationId)
        ));

    int readCount = directMessageRepository.markUnreadMessagesAsReadUntil(
        conversationId,
        currentUserId,
        readUntilMessage.getCreatedAt(),
        readUntilMessage.getId()
    );

    ConversationUserState state = Optional.ofNullable(conversationUserStateRepository
        .findByConversationIdAndUserId(conversationId, currentUserId))
        .flatMap(optionalState -> optionalState)
        .orElseGet(() -> createState(conversation, currentUserId));
    state.markAsRead(readUntilMessage.getId(), readUntilMessage.getCreatedAt());
    state.decreaseUnreadCount(readCount);

    log.debug(
        "DM 읽음 처리를 완료했습니다. conversationId={}, readUntilMessageId={}, readCount={}",
        conversationId,
        directMessageId,
        readCount
    );
  }

  @Transactional(readOnly = true)
  public CursorResponse<DirectMessageDto> findDms(
      UUID conversationId,
      DirectMessageFindAllRequest request,
      UUID currentUserId
  ) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationException(
            ConversationErrorCode.CONVERSATION_NOT_FOUND,
            Map.of("conversationId", conversationId)
        ));
    validateParticipant(conversation, currentUserId);

    User currentUser = findUser(currentUserId);
    User withUser = findUser(resolveWithUserId(conversation, currentUserId));
    UserSummaryDto currentUserSummary = toUserSummaryDto(currentUser);
    UserSummaryDto withUserSummary = toUserSummaryDto(withUser);
    List<DirectMessage> directMessages = directMessageRepository.findAllWithCursor(
        request,
        conversationId
    );
    long totalCount = directMessageRepository.countWithCondition(conversationId);
    boolean hasNext = directMessages.size() > request.limit();

    if (hasNext) {
      directMessages = directMessages.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext) {
      DirectMessage last = directMessages.get(directMessages.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    List<DirectMessageDto> data = directMessages.stream()
        .map(directMessage -> directMessageMapper.toDto(
            directMessage,
            conversation,
            currentUserId,
            currentUserSummary,
            withUserSummary
        ))
        .toList();

    return new CursorResponse<>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );
  }

  private ConversationDto toConversationDto(
      ConversationListItemDto conversation,
      UUID currentUserId,
      UserSummaryDto currentUser
  ) {
    UserSummaryDto withUser = toUserSummaryDto(conversation.withUser());
    return new ConversationDto(
        conversation.id(),
        withUser,
        toLatestMessageDto(conversation, currentUserId, currentUser, withUser),
        conversation.unreadCount() > 0L
    );
  }

  private ConversationDto.DirectMessageDto toLatestMessageDto(
      ConversationListItemDto conversation,
      UUID currentUserId,
      UserSummaryDto currentUser,
      UserSummaryDto withUser
  ) {
    if (conversation.lastMessageId() == null) {
      return null;
    }

    UUID senderId = conversation.lastMessageSenderId();
    UserSummaryDto sender = currentUserId.equals(senderId) ? currentUser : withUser;
    UserSummaryDto receiver = currentUserId.equals(senderId) ? withUser : currentUser;

    return new ConversationDto.DirectMessageDto(
        conversation.lastMessageId(),
        conversation.id(),
        conversation.lastMessageAt(),
        sender,
        receiver,
        conversation.lastMessageContent()
    );
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

  private UserSummaryDto toUserSummaryDto(UserDto user) {
    return new UserSummaryDto(user.getId(), user.getName(), user.getProfileImageUrl());
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

  private ConversationDto toDto(Conversation conversation, UUID currentUserId) {
    User currentUser = findUser(currentUserId);
    User withUser = findUser(resolveWithUserId(conversation, currentUserId));

    return toDto(conversation, currentUser, withUser);
  }

  private ConversationDto toDto(Conversation conversation, User currentUser, User withUser) {
    ConversationUserState state = Optional.ofNullable(conversationUserStateRepository
        .findByConversationIdAndUserId(conversation.getId(), currentUser.getId()))
        .flatMap(optionalState -> optionalState)
        .orElse(null);
    ConversationListItemDto item = new ConversationListItemDto(
        conversation.getId(),
        conversation.getLastMessageAt() != null ? conversation.getLastMessageAt() : conversation.getCreatedAt(),
        withUser,
        conversation.getLastMessageId(),
        conversation.getLastMessageAt(),
        conversation.getLastMessageContent(),
        conversation.getLastMessageSenderId(),
        state != null ? state.getUnreadCount() : 0L
    );

    return toConversationDto(item, currentUser.getId(), toUserSummaryDto(currentUser));
  }
}
