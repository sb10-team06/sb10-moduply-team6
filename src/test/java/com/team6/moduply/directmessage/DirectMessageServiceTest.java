package com.team6.moduply.directmessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.team6.moduply.directmessage.service.DirectMessageService;
import com.team6.moduply.notification.event.DirectMessageReceivedEvent;
import com.team6.moduply.user.dto.UserSummaryDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

  @InjectMocks
  private DirectMessageService directMessageService;

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private DirectMessageRepository directMessageRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private DirectMessageMapper directMessageMapper;

  @Mock
  private BinaryContentService binaryContentService;

  @Mock
  private UserMapper userMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  @DisplayName("대화방 참여자가 DM을 생성하면 메시지를 저장하고 DTO를 반환한다.")
  void create_success_with_participant() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    User currentUser = user(currentUserId, "current");
    User withUser = user(withUserId, "with");
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");
    DirectMessage savedDirectMessage = DirectMessage.create(conversation, currentUser, request.content());
    ReflectionTestUtils.setField(savedDirectMessage, "id", directMessageId);
    ReflectionTestUtils.setField(savedDirectMessage, "createdAt", createdAt);
    UserSummaryDto currentUserSummary = new UserSummaryDto(currentUserId, "current", null);
    UserSummaryDto withUserSummary = new UserSummaryDto(withUserId, "with", null);
    DirectMessageDto expected = new DirectMessageDto(
        directMessageId,
        conversationId,
        createdAt,
        currentUserSummary,
        withUserSummary,
        "hello"
    );

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
    given(binaryContentService.generateUrl(currentUser.getProfileImg())).willReturn(null);
    given(binaryContentService.generateUrl(withUser.getProfileImg())).willReturn(null);
    given(userMapper.toSummaryDto(currentUser, null)).willReturn(currentUserSummary);
    given(userMapper.toSummaryDto(withUser, null)).willReturn(withUserSummary);
    given(directMessageRepository.save(any(DirectMessage.class)))
        .willReturn(savedDirectMessage);
    given(directMessageMapper.toDto(
        savedDirectMessage,
        conversation,
        currentUserId,
        currentUserSummary,
        withUserSummary
    )).willReturn(expected);

    DirectMessageDto result = directMessageService.create(conversationId, request, currentUserId);

    assertThat(result).isEqualTo(expected);
    ArgumentCaptor<DirectMessage> directMessageCaptor = ArgumentCaptor.forClass(
        DirectMessage.class
    );
    verify(directMessageRepository).save(directMessageCaptor.capture());
    DirectMessage capturedDirectMessage = directMessageCaptor.getValue();
    assertThat(capturedDirectMessage.getContent()).isEqualTo(request.content());
    assertThat(capturedDirectMessage.getConversation()).isEqualTo(conversation);
    assertThat(capturedDirectMessage.getSender()).isEqualTo(currentUser);
    ArgumentCaptor<DirectMessageReceivedEvent> eventCaptor = ArgumentCaptor.forClass(
        DirectMessageReceivedEvent.class
    );
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    DirectMessageReceivedEvent event = eventCaptor.getValue();
    assertThat(event.getReceiverId()).isEqualTo(withUserId);
    assertThat(event.getSenderId()).isEqualTo(currentUserId);
    assertThat(event.getSenderName()).isEqualTo(currentUser.getName());
    assertThat(event.getConversationId()).isEqualTo(conversationId);
  }

  @Test
  @DisplayName("대화방 참여자가 아닌 사용자가 DM을 생성하면 예외가 발생한다.")
  void create_fail_when_not_participant() {
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    Conversation conversation = Conversation.create(user1Id, user2Id);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    assertThatThrownBy(() -> directMessageService.create(conversationId, request, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_FORBIDDEN);
          assertThat(exception.getDetails().get("conversationId")).isEqualTo(conversationId);
          assertThat(exception.getDetails().get("userId")).isEqualTo(currentUserId);
        });

    verify(directMessageRepository, never()).save(any(DirectMessage.class));
  }

  @Test
  @DisplayName("존재하지 않는 대화방에 DM을 생성하면 예외가 발생한다.")
  void create_fail_when_conversation_not_found() {
    UUID currentUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> directMessageService.create(conversationId, request, currentUserId))
        .isInstanceOfSatisfying(ConversationException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ConversationErrorCode.CONVERSATION_NOT_FOUND);
          assertThat(exception.getDetails().get("conversationId")).isEqualTo(conversationId);
        });

    verify(directMessageRepository, never()).save(any(DirectMessage.class));
  }

  @Test
  @DisplayName("DM 생성 요청자가 존재하지 않으면 예외가 발생한다.")
  void create_fail_when_current_user_not_found() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(userRepository.findById(currentUserId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> directMessageService.create(conversationId, request, currentUserId))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(currentUserId);
        });

    verify(directMessageRepository, never()).save(any(DirectMessage.class));
  }

  @Test
  @DisplayName("DM 수신자가 존재하지 않으면 예외가 발생한다.")
  void create_fail_when_with_user_not_found() {
    UUID currentUserId = UUID.randomUUID();
    UUID withUserId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    User currentUser = user(currentUserId, "current");
    Conversation conversation = Conversation.create(currentUserId, withUserId);
    ReflectionTestUtils.setField(conversation, "id", conversationId);
    DirectMessageCreateRequest request = new DirectMessageCreateRequest("hello");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
    given(userRepository.findById(withUserId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> directMessageService.create(conversationId, request, currentUserId))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(withUserId);
        });

    verify(directMessageRepository, never()).save(any(DirectMessage.class));
  }

  private User user(UUID userId, String name) {
    User user = new User(name + "@example.com", "password", name, Role.USER);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
