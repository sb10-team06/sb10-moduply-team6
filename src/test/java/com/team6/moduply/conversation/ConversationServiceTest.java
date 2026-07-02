package com.team6.moduply.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.exception.ConversationErrorCode;
import com.team6.moduply.conversation.exception.ConversationException;
import com.team6.moduply.conversation.mapper.ConversationMapper;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.conversation.service.ConversationService;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.exception.DirectMessageErrorCode;
import com.team6.moduply.directmessage.exception.DirectMessageException;
import com.team6.moduply.directmessage.repository.DirectMessageRepository;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

  @InjectMocks
  private ConversationService conversationService;

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private DirectMessageRepository directMessageRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ConversationMapper conversationMapper;

  @Test
  @DisplayName("대화 상대가 존재하고 기존 대화방이 없으면 새 대화방을 생성한다.")
  void create_success_with_valid_users() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    User currentUser = user(currentUserId, "current");
    User withUser = user(withUserId, "with");
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ConversationDto expected = new ConversationDto(conversation.getId(), null, null, false);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
    given(conversationRepository.findByUser1IdAndUser2Id(conversation.getUser1Id(), conversation.getUser2Id()))
        .willReturn(Optional.empty());
    given(conversationRepository.saveAndFlush(any(Conversation.class))).willReturn(conversation);
    given(conversationMapper.toCreateDto(conversation, currentUser, withUser)).willReturn(expected);

    ConversationDto result = conversationService.create(request, currentUserId);

    assertThat(result).isEqualTo(expected);
    verify(conversationRepository).saveAndFlush(any(Conversation.class));
  }

  @Test
  @DisplayName("이미 같은 두 사용자 간 대화방이 있으면 충돌 예외가 발생한다.")
  void create_fail_when_conversation_already_exists() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    User currentUser = user(currentUserId, "current");
    User withUser = user(withUserId, "with");
    Conversation conversation = Conversation.create(currentUserId, withUserId);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
    given(conversationRepository.findByUser1IdAndUser2Id(conversation.getUser1Id(), conversation.getUser2Id()))
        .willReturn(Optional.of(conversation));

    assertThatThrownBy(() -> conversationService.create(request, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_ALREADY_EXISTS);
          assertThat(exception.getDetails().get("user1Id")).isEqualTo(conversation.getUser1Id());
          assertThat(exception.getDetails().get("user2Id")).isEqualTo(conversation.getUser2Id());
        });

    verify(conversationRepository, never()).saveAndFlush(any(Conversation.class));
  }

  @Test
  @DisplayName("동시에 같은 대화방이 생성되어 저장 충돌이 발생하면 충돌 예외가 발생한다.")
  void create_fail_when_concurrent_create_conflicts() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    User currentUser = user(currentUserId, "current");
    User withUser = user(withUserId, "with");
    Conversation conversation = Conversation.create(currentUserId, withUserId);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
    given(conversationRepository.findByUser1IdAndUser2Id(conversation.getUser1Id(), conversation.getUser2Id()))
        .willReturn(Optional.empty());
    given(conversationRepository.saveAndFlush(any(Conversation.class)))
        .willThrow(new DataIntegrityViolationException("duplicated conversation"));

    assertThatThrownBy(() -> conversationService.create(request, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_ALREADY_EXISTS);
          assertThat(exception.getDetails().get("user1Id")).isEqualTo(conversation.getUser1Id());
          assertThat(exception.getDetails().get("user2Id")).isEqualTo(conversation.getUser2Id());
        });

    verify(conversationRepository).saveAndFlush(any(Conversation.class));
  }

  @Test
  @DisplayName("자기 자신과 대화방을 생성하면 예외가 발생한다.")
  void create_fail_when_with_user_is_self() {
    UUID userId = UUID.randomUUID();
    ConversationCreateRequest request = new ConversationCreateRequest(userId);

    assertThatThrownBy(() -> conversationService.create(request, userId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository, never()).findById(any(UUID.class));
    verify(conversationRepository, never()).saveAndFlush(any(Conversation.class));
  }

  @Test
  @DisplayName("대화 상대가 존재하지 않으면 예외가 발생한다.")
  void create_fail_when_with_user_not_found() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    User currentUser = user(currentUserId, "current");

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> conversationService.create(request, currentUserId))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(withUserId);
        });

    verify(conversationRepository, never()).saveAndFlush(any(Conversation.class));
  }

  @Test
  @DisplayName("대화방 참여자가 대화방을 조회하면 대화방 정보를 반환한다.")
  void findById_success_with_participant() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    User currentUser = user(currentUserId, "current");
    User withUser = user(withUserId, "with");
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    ConversationDto expected = new ConversationDto(conversationId, null, null, true);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
    given(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId))
        .willReturn(Optional.empty());
    given(directMessageRepository.existsByConversationIdAndSenderIdNotAndReadFalse(
        conversationId,
        currentUserId
    )).willReturn(true);
    given(conversationMapper.toDto(conversation, currentUser, withUser, null, true)).willReturn(expected);

    ConversationDto result = conversationService.findById(conversationId, currentUserId);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("존재하지 않는 대화방을 조회하면 예외가 발생한다.")
  void findById_fail_when_conversation_not_found() {
    UUID conversationId = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();

    given(conversationRepository.findById(conversationId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> conversationService.findById(conversationId, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_NOT_FOUND);
          assertThat(exception.getDetails().get("conversationId")).isEqualTo(conversationId);
        });

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("대화방 참여자가 아닌 사용자가 대화방을 조회하면 예외가 발생한다.")
  void findById_fail_when_not_participant() {
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    Conversation conversation = Conversation.create(user1Id, user2Id);
    ReflectionTestUtils.setField(conversation, "id", conversationId);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    assertThatThrownBy(() -> conversationService.findById(conversationId, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_FORBIDDEN);
          assertThat(exception.getDetails().get("conversationId")).isEqualTo(conversationId);
          assertThat(exception.getDetails().get("userId")).isEqualTo(currentUserId);
        });

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("특정 사용자와의 대화가 존재하면 대화 정보를 반환한다.")
  void findByUserId_success_with_existing_conversation() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    User currentUser = user(currentUserId, "current");
    User withUser = user(withUserId, "with");
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    ConversationDto expected = new ConversationDto(conversationId, null, null, true);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
    given(conversationRepository.findByUser1IdAndUser2Id(conversation.getUser1Id(), conversation.getUser2Id()))
        .willReturn(Optional.of(conversation));
    given(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId))
        .willReturn(Optional.empty());
    given(directMessageRepository.existsByConversationIdAndSenderIdNotAndReadFalse(
        conversationId,
        currentUserId
    )).willReturn(true);
    given(conversationMapper.toDto(conversation, currentUser, withUser, null, true)).willReturn(expected);

    ConversationDto result = conversationService.findByUserId(withUserId, currentUserId);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("특정 사용자와의 대화가 존재하지 않으면 예외가 발생한다.")
  void findByUserId_fail_when_conversation_not_found() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    User currentUser = user(currentUserId, "current");
    User withUser = user(withUserId, "with");
    Conversation sortedConversation = Conversation.create(currentUserId, withUserId);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
    given(conversationRepository.findByUser1IdAndUser2Id(
        sortedConversation.getUser1Id(),
        sortedConversation.getUser2Id()
    )).willReturn(Optional.empty());

    assertThatThrownBy(() -> conversationService.findByUserId(withUserId, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_NOT_FOUND);
          assertThat(exception.getDetails().get("userId")).isEqualTo(withUserId);
        });
  }

  @Test
  @DisplayName("자기 자신과의 대화를 조회하면 예외가 발생한다.")
  void findByUserId_fail_when_user_is_self() {
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() -> conversationService.findByUserId(userId, userId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("DM 수신자가 읽음 처리를 요청하면 메시지를 읽음 상태로 변경한다.")
  void read_success_with_receiver() {
    UUID currentUserId = UUID.randomUUID();
    UUID senderId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();
    User sender = user(senderId, "sender");
    Conversation conversation = Conversation.create(currentUserId, senderId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    DirectMessage directMessage = org.mockito.Mockito.mock(DirectMessage.class);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(directMessageRepository.findByIdAndConversationId(directMessageId, conversationId))
        .willReturn(Optional.of(directMessage));
    given(directMessage.getSender()).willReturn(sender);

    conversationService.read(conversationId, directMessageId, currentUserId);

    verify(directMessage).markAsRead();
  }

  @Test
  @DisplayName("존재하지 않는 대화방의 DM 읽음 처리를 요청하면 예외가 발생한다.")
  void read_fail_when_conversation_not_found() {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();

    given(conversationRepository.findById(conversationId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> conversationService.read(conversationId, directMessageId, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_NOT_FOUND);
          assertThat(exception.getDetails().get("conversationId")).isEqualTo(conversationId);
        });

    verify(directMessageRepository, never()).findByIdAndConversationId(any(), any());
  }

  @Test
  @DisplayName("대화방 참여자가 아닌 사용자가 DM 읽음 처리를 요청하면 예외가 발생한다.")
  void read_fail_when_not_participant() {
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();
    Conversation conversation = Conversation.create(user1Id, user2Id);
    ReflectionTestUtils.setField(conversation, "id", conversationId);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    assertThatThrownBy(() -> conversationService.read(conversationId, directMessageId, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_FORBIDDEN);
          assertThat(exception.getDetails().get("conversationId")).isEqualTo(conversationId);
          assertThat(exception.getDetails().get("userId")).isEqualTo(currentUserId);
        });

    verify(directMessageRepository, never()).findByIdAndConversationId(any(), any());
  }

  @Test
  @DisplayName("대화방에 속하지 않는 DM 읽음 처리를 요청하면 예외가 발생한다.")
  void read_fail_when_direct_message_not_found() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(directMessageRepository.findByIdAndConversationId(directMessageId, conversationId))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> conversationService.read(conversationId, directMessageId, currentUserId))
        .isInstanceOfSatisfying(DirectMessageException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(DirectMessageErrorCode.DIRECT_MESSAGE_NOT_FOUND);
          assertThat(exception.getDetails().get("directMessageId")).isEqualTo(directMessageId);
          assertThat(exception.getDetails().get("conversationId")).isEqualTo(conversationId);
        });
  }

  @Test
  @DisplayName("DM 발신자가 본인 메시지 읽음 처리를 요청하면 예외가 발생한다.")
  void read_fail_when_sender_requests() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();
    User sender = user(currentUserId, "sender");
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    DirectMessage directMessage = org.mockito.Mockito.mock(DirectMessage.class);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(directMessageRepository.findByIdAndConversationId(directMessageId, conversationId))
        .willReturn(Optional.of(directMessage));
    given(directMessage.getSender()).willReturn(sender);

    assertThatThrownBy(() -> conversationService.read(conversationId, directMessageId, currentUserId))
        .isInstanceOfSatisfying(DirectMessageException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(DirectMessageErrorCode.DIRECT_MESSAGE_FORBIDDEN);
          assertThat(exception.getDetails().get("directMessageId")).isEqualTo(directMessageId);
          assertThat(exception.getDetails().get("userId")).isEqualTo(currentUserId);
        });

    verify(directMessage, never()).markAsRead();
  }

  private User user(UUID userId, String name) {
    User user = new User(name + "@example.com", "password", name, Role.USER);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
