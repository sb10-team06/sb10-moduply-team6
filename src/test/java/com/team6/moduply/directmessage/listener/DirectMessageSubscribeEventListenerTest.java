package com.team6.moduply.directmessage.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.common.websocket.events.StompSubscribeEvent;
import com.team6.moduply.conversation.repository.ConversationRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageDeliveryException;

@ExtendWith(MockitoExtension.class)
class DirectMessageSubscribeEventListenerTest {

  @InjectMocks
  private DirectMessageSubscribeEventListener directMessageSubscribeEventListener;

  @Mock
  private ConversationRepository conversationRepository;

  @Test
  @DisplayName("DM 구독 요청자가 대화방 참여자이면 구독을 허용한다.")
  void on_success_when_direct_message_subscriber_is_participant() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    StompSubscribeEvent event = event(
        userId,
        "/sub/conversations/" + conversationId + "/direct-messages"
    );

    given(conversationRepository.existsByIdAndParticipantId(conversationId, userId))
        .willReturn(true);

    assertThatCode(() -> directMessageSubscribeEventListener.on(event))
        .doesNotThrowAnyException();

    verify(conversationRepository).existsByIdAndParticipantId(conversationId, userId);
  }

  @Test
  @DisplayName("DM 구독 요청자가 대화방 참여자가 아니면 구독을 거부한다.")
  void on_fail_when_direct_message_subscriber_is_not_participant() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    StompSubscribeEvent event = event(
        userId,
        "/sub/conversations/" + conversationId + "/direct-messages"
    );

    given(conversationRepository.existsByIdAndParticipantId(conversationId, userId))
        .willReturn(false);

    assertThatThrownBy(() -> directMessageSubscribeEventListener.on(event))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("대화방 참여자만 DM을 구독할 수 있습니다.");

    verify(conversationRepository).existsByIdAndParticipantId(conversationId, userId);
  }

  @Test
  @DisplayName("DM 구독 경로의 대화방 ID 형식이 잘못되면 구독을 거부한다.")
  void on_fail_when_direct_message_destination_has_invalid_conversation_id() {
    UUID userId = UUID.randomUUID();
    StompSubscribeEvent event = event(
        userId,
        "/sub/conversations/not-a-uuid/direct-messages"
    );

    assertThatThrownBy(() -> directMessageSubscribeEventListener.on(event))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessageContaining("올바르지 않은 대화방 ID 형식입니다.");

    verify(conversationRepository, never()).existsByIdAndParticipantId(
        org.mockito.ArgumentMatchers.any(UUID.class),
        org.mockito.ArgumentMatchers.any(UUID.class)
    );
  }

  @Test
  @DisplayName("DM이 아닌 구독 요청은 처리하지 않는다.")
  void on_success_when_not_direct_message_subscription() {
    StompSubscribeEvent event = event(UUID.randomUUID(), "/sub/contents/%s/watch".formatted(UUID.randomUUID()));

    assertThatCode(() -> directMessageSubscribeEventListener.on(event))
        .doesNotThrowAnyException();

    verify(conversationRepository, never()).existsByIdAndParticipantId(
        org.mockito.ArgumentMatchers.any(UUID.class),
        org.mockito.ArgumentMatchers.any(UUID.class)
    );
  }

  private StompSubscribeEvent event(UUID userId, String destination) {
    return new StompSubscribeEvent("session-1", userId, destination);
  }
}
