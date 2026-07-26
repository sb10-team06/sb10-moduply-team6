package com.team6.moduply.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.common.redis.RedisSubscriber;
import com.team6.moduply.sse.SseRedisSubscriber;
import com.team6.moduply.watching.model.WatchingSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  // 💡 RedisTemplate<String, WatchingSession> 빈 등록
  @Bean
  public RedisTemplate<String, WatchingSession> watchingSessionRedisTemplate(
      RedisConnectionFactory connectionFactory,
      ObjectMapper objectMapper) {
    RedisTemplate<String, WatchingSession> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    Jackson2JsonRedisSerializer<WatchingSession> jacksonSerializer =
        new Jackson2JsonRedisSerializer<>(objectMapper, WatchingSession.class);

    template.setValueSerializer(jacksonSerializer);
    template.setHashValueSerializer(jacksonSerializer);
    template.afterPropertiesSet();
    return template;
  }

  // TODO: StringRedisTemplate으로 대체 가능하면 이 빈은 제거 검토
  // 기존 RedisTemplate<String, String> 주입 코드가 있을 수 있어 현재는 유지한다.
  @Bean
  public RedisTemplate<String, String> sessionIdAndUserIdRedisTemplate(
      RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }

  // Redis pub/sub
  @Bean
  public RedisTemplate<String, Object> redisPubSubTemplate(
      RedisConnectionFactory factory,
      ObjectMapper mapper) {

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(mapper, Object.class));
    return template;
  }

  // 리스너
  @Bean
  @Profile("!test")
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      RedisSubscriber redisSubscriber,
      SseRedisSubscriber sseRedisSubscriber,
      @Qualifier(AsyncConfig.REDIS_MESSAGE_TASK_EXECUTOR) TaskExecutor taskExecutor) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setTaskExecutor(taskExecutor);

    // /sub으로 시작하는 모든 경로 구독
    container.addMessageListener(redisSubscriber, new PatternTopic("/sub/*"));

    // SSE 알림 구독
    container.addMessageListener(sseRedisSubscriber, new PatternTopic("sse:notification:*"));

    return container;
  }
}
