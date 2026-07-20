package com.team6.moduply.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

  @Qualifier("redisPubSubTemplate")
  private final RedisTemplate<String, Object> redisPubsubTemplate;

  public void publish(String topic, Object message) {
    redisPubsubTemplate.convertAndSend(topic, message);
    log.trace("레디스 메세지 발행: topic={}, message={}", topic, message);
  }
}