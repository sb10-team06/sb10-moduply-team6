package com.team6.moduply.conversation.service;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.dto.ConversationFindAllRequest;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.mapper.ConversationMapper;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.dto.DirectMessageFindAllRequest;
import com.team6.moduply.directmessage.exception.DirectMessageErrorCode;
import com.team6.moduply.directmessage.exception.DirectMessageException;
import com.team6.moduply.directmessage.mapper.DirectMessageMapper;
import com.team6.moduply.directmessage.repository.DirectMessageRepository;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final DirectMessageMapper directMessageMapper;
  private final BinaryContentService binaryContentService;
  private final UserMapper userMapper;

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

  @Transactional(readOnly = true)
  public CursorResponse<ConversationDto> findAll(
      ConversationFindAllRequest request,
      UUID currentUserId
  ) {
    log.debug(
        "대화 목록 조회 처리 시작. currentUserId={}, keywordLike={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
        currentUserId,
        request.keywordLike(),
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortBy(),
        request.sortDirection()
    );

    User currentUser = findUser(currentUserId);
    List<Conversation> conversations = conversationRepository.findAllWithCursor(request, currentUserId);
    long totalCount = conversationRepository.countWithCondition(request, currentUserId);
    boolean hasNext = conversations.size() > request.limit();

    if (hasNext) {
      conversations = conversations.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      Conversation last = conversations.get(conversations.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }
    // (사용자ID, 사용자)
    Map<UUID, User> usersById = findUsersById(resolveWithUserIds(conversations, currentUserId));
    // (대화방ID, 가장최근DM)
    Map<UUID, DirectMessage> latestMessagesByConversationId = findLatestMessagesByConversationId(
        conversations
    );
    // 읽지않은 DM이 존재하는 대화방 ID들
    Set<UUID> unreadConversationIds = findUnreadConversationIds(conversations, currentUserId);

    // 대화방돌면서 Dto 형태로 매핑
    List<ConversationDto> data = conversations.stream()
        .map(conversation -> conversationMapper.toDto(
            conversation,
            currentUser,
            usersById.get(resolveWithUserId(conversation, currentUserId)),
            latestMessagesByConversationId.get(conversation.getId()),
            unreadConversationIds.contains(conversation.getId())
        ))
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
    log.debug("대화 목록 조회 처리 완료. count={}, hasNext={}", data.size(), hasNext);
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

    log.debug(
            "DM 읽음 처리 완료. conversationId={}, readUntilMessageId={}, readCount={}",
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
    log.debug(
        "DM 목록 조회 처리 시작. conversationId={}, currentUserId={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
        conversationId,
        currentUserId,
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortBy(),
        request.sortDirection()
    );
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

    CursorResponse<DirectMessageDto> response = new CursorResponse<>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );
    log.debug("DM 목록 조회 처리 완료. count={}, hasNext={}", data.size(), hasNext);
    return response;
  }

  private Set<UUID> resolveWithUserIds(List<Conversation> conversations, UUID currentUserId) {
    return conversations.stream()
        .map(conversation -> resolveWithUserId(conversation, currentUserId))
        .collect(Collectors.toSet());
  }

  private Map<UUID, User> findUsersById(Set<UUID> userIds) {
    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }
    // User들 돌면서 Map<UUID, User> 형태로
    // User(id=aaaa-aaa..., name="인성"), ...
    Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

    // userIds중 실제로 DB에 있는지 검증
    userIds.stream()
        .filter(userId -> !usersById.containsKey(userId))
        .findFirst()
        .ifPresent(userId -> {
          throw new UserException(
              UserErrorCode.USER_NOT_FOUND_EXCEPTION,
              Map.of("userId", userId)
          );
        });

    return usersById;
  }

  /// 각 대화방의 최근 DM을 MAP 형태로 (대화방ID : 가장 최근 DM)
  private Map<UUID, DirectMessage> findLatestMessagesByConversationId(
      List<Conversation> conversations
  ) {
    // 대화방 ID 리스트로 만들고
    List<UUID> conversationIds = resolveConversationIds(conversations);
    if (conversationIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return directMessageRepository.findLatestMessagesByConversationIds(conversationIds).stream()
        .collect(Collectors.toMap(
            message -> message.getConversation().getId(),
            Function.identity(),
            (first, ignored) -> first
        ));
  }

  /// 읽지않은 DM이 존재하는 대화방ID들
  private Set<UUID> findUnreadConversationIds(
      List<Conversation> conversations,
      UUID currentUserId
  ) {
    List<UUID> conversationIds = resolveConversationIds(conversations);
    if (conversationIds.isEmpty()) {
      return Collections.emptySet();
    }

    return new HashSet<>(directMessageRepository.findUnreadConversationIds(
        conversationIds,
        currentUserId
    ));
  }

  private List<UUID> resolveConversationIds(List<Conversation> conversations) {
    return conversations.stream()
        .map(Conversation::getId)
        .toList();
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
