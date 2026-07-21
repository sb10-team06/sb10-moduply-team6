package com.team6.moduply.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseRedisPublisher {

  @Qualifier("redisPubSubTemplate")
  private final RedisTemplate<String, Object> redisTemplate;

  private static final String SSE_CHANNEL_PREFIX = "sse:notification:";

  public void publish(UUID userId, Object data) {
    String channel = SSE_CHANNEL_PREFIX + userId;
    redisTemplate.convertAndSend(channel, data);
    log.debug("[SSE Redis] 메시지 발행 - channel={}", channel);
  }
}
