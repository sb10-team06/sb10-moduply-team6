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
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
  @DisplayName("대화 상대가 존재하면 새 대화방을 생성하고 응답을 반환한다.")
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
    given(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId()))
        .willReturn(Optional.empty());
    given(directMessageRepository.existsByConversationIdAndSenderIdNotAndReadFalse(
        conversation.getId(),
        currentUserId
    )).willReturn(false);
    given(conversationMapper.toDto(conversation, currentUser, withUser, null, false)).willReturn(expected);

    ConversationDto result = conversationService.create(request, currentUserId);

    assertThat(result).isEqualTo(expected);
    verify(conversationRepository).saveAndFlush(any(Conversation.class));
  }

  @Test
  @DisplayName("이미 같은 두 사용자 간 대화방이 있으면 기존 대화방 응답을 반환한다.")
  void create_success_when_conversation_already_exists() {
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
        .willReturn(Optional.of(conversation));
    given(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId()))
        .willReturn(Optional.empty());
    given(directMessageRepository.existsByConversationIdAndSenderIdNotAndReadFalse(
        conversation.getId(),
        currentUserId
    )).willReturn(false);
    given(conversationMapper.toDto(conversation, currentUser, withUser, null, false)).willReturn(expected);

    ConversationDto result = conversationService.create(request, currentUserId);

    assertThat(result).isEqualTo(expected);
    verify(conversationRepository, never()).saveAndFlush(any(Conversation.class));
  }

  @Test
  @DisplayName("동시에 같은 대화방이 생성되어 저장 충돌이 발생하면 기존 대화방을 다시 조회해 응답을 반환한다.")
  void create_success_when_concurrent_create_conflicts() {
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
        .willReturn(Optional.empty(), Optional.of(conversation));
    given(conversationRepository.saveAndFlush(any(Conversation.class)))
        .willThrow(new DataIntegrityViolationException("duplicated conversation"));
    given(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId()))
        .willReturn(Optional.empty());
    given(directMessageRepository.existsByConversationIdAndSenderIdNotAndReadFalse(
        conversation.getId(),
        currentUserId
    )).willReturn(false);
    given(conversationMapper.toDto(conversation, currentUser, withUser, null, false)).willReturn(expected);

    ConversationDto result = conversationService.create(request, currentUserId);

    assertThat(result).isEqualTo(expected);
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

  private User user(UUID userId, String name) {
    User user = new User(name + "@example.com", "password", name, Role.USER);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
