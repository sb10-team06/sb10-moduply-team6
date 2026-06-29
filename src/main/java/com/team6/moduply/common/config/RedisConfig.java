package com.team6.moduply.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.watching.model.WatchingSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
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

}
