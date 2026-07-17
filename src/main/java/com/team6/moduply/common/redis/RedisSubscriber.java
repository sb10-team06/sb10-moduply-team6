package com.team6.moduply.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

  private final SimpMessagingTemplate messagingTemplate;
  @Qualifier("redisPubSubTemplate")
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void onMessage(Message message, @Nullable byte[] pattern) {
    try {
      Object body = redisTemplate.getValueSerializer().deserialize(message.getBody());
      String destination = new String(message.getChannel());

      // STOMP로 다시 전달
      messagingTemplate.convertAndSend(destination, body);
      log.info("레디스 메세지 전송: destination={}, body={}", destination, body);

    } catch (Exception e) {
      log.error("Redis Pub/Sub 메시지 전송 실패: {}", e.getMessage(), e);
    }
  }
}