package com.team6.moduply.config;

import com.team6.moduply.common.redis.RedisSubscriber;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@TestConfiguration
public class TestRedisConfig {

  @Bean
  @Primary
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory redisConnectionFactory,
      RedisSubscriber redisSubscriber) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory); // 💡 Testcontainers의 팩토리를 주입
    container.addMessageListener(redisSubscriber, new PatternTopic("/sub/**"));
    return container;
  }
}