package com.team6.moduply.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseRedisSubscriber implements MessageListener {

  private final SseEmitterManager sseEmitterManager;
  private final ObjectMapper objectMapper;

  private static final String SSE_CHANNEL_PREFIX = "sse:notification:";

  @Override
  public void onMessage(Message message, @Nullable byte[] pattern) {
    try {
      String channel = new String(message.getChannel());
      String userId = channel.replace(SSE_CHANNEL_PREFIX, "");

      Object data = objectMapper.readValue(message.getBody(), Object.class);
      sseEmitterManager.send(UUID.fromString(userId), data);

      log.debug("[SSE Redis] 메시지 수신 - channel={}", channel);
    } catch (Exception e) {
      log.error("[SSE Redis] 메시지 처리 실패: {}", e.getMessage(), e);
    }
  }
}
