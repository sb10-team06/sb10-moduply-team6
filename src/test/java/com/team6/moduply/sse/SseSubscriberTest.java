package com.team6.moduply.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.enums.NotificationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SseRedisSubscriberTest {

  @InjectMocks
  private SseRedisSubscriber sseRedisSubscriber;

  @Mock
  private SseEmitterManager sseEmitterManager;

  @Mock
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Redis 메시지 수신 시 SSE로 전송한다.")
  void onMessage_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    String channel = "sse:notification:" + userId;

    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(), Instant.now(), userId, "제목", "내용", NotificationLevel.INFO);

    Message message = mock(Message.class);
    given(message.getChannel()).willReturn(channel.getBytes());
    given(message.getBody()).willReturn("{}".getBytes());
    given(objectMapper.readValue(any(byte[].class), eq(NotificationDto.class))).willReturn(dto);

    // when
    sseRedisSubscriber.onMessage(message, null);

    // then
    verify(sseEmitterManager).send(userId, dto);
  }

  @Test
  @DisplayName("Redis 메시지 처리 실패 시 예외가 전파되지 않는다.")
  void onMessage_fail() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    String channel = "sse:notification:" + userId;

    Message message = mock(Message.class);
    given(message.getChannel()).willReturn(channel.getBytes());
    given(message.getBody()).willReturn("{}".getBytes());
    given(objectMapper.readValue(any(byte[].class), eq(NotificationDto.class)))
        .willThrow(new RuntimeException("역직렬화 실패"));

    // when & then
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> sseRedisSubscriber.onMessage(message, null));
  }
}
