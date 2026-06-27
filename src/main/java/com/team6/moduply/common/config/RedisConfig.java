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

  // 주선 처리 한 이유 -> StringRedisTemplate으로 완전 대체 가능하기 때문에 굳이 RedisTemplate<String, String>을 등록할 필요가 없음

  // 💡 RedisTemplate<String, String> 빈 등록 (세션아이디-사용자아이디)
//  @Bean
//  public RedisTemplate<String, String> sessionIdAndUserIdRedisTemplate(
//      RedisConnectionFactory connectionFactory) {
//    RedisTemplate<String, String> template = new RedisTemplate<>();
//    template.setConnectionFactory(connectionFactory);
//    template.setKeySerializer(new StringRedisSerializer());
//    template.setValueSerializer(new StringRedisSerializer());
//    template.setHashKeySerializer(new StringRedisSerializer());
//    template.setHashValueSerializer(new StringRedisSerializer());
//    template.afterPropertiesSet();
//    return template;
//  }

}
