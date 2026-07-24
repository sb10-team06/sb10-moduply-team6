package com.team6.moduply.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

  public static final String USER_SUMMARY = "userSummary";
  public static final String CONTENT_LIST = "contentList";
  public static final String CONTENT_DETAIL = "contentDetail";
  public static final String CONTENT_TAGS = "contentTags";
  public static final String PROFILE_IMAGE_URL = "profileImageUrl";
  public static final String PLAYLIST_DETAIL = "playlistDetail";
  public static final String FOLLOW_COUNT = "followCount";
  public static final String CONTENT_RANKING = "contentRanking";
  public static final String REVIEW_LIST = "reviewList";

  @Bean
  public CacheManager cacheManager(
      RedisConnectionFactory connectionFactory,
      ObjectMapper objectMapper
  ) {
    // TTL: 5분
    RedisCacheConfiguration defaultConfig = redisCacheConfiguration(
        objectMapper,
        Duration.ofMinutes(5)
    );

    Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
        USER_SUMMARY, defaultConfig.entryTtl(Duration.ofMinutes(5)),
        CONTENT_LIST, defaultConfig.entryTtl(Duration.ofMinutes(5)),
        CONTENT_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(5)),
        CONTENT_TAGS, defaultConfig.entryTtl(Duration.ofMinutes(10)),
        PROFILE_IMAGE_URL, defaultConfig.entryTtl(Duration.ofMinutes(5)),
        PLAYLIST_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(5)),
        FOLLOW_COUNT, defaultConfig.entryTtl(Duration.ofSeconds(30)),       //  Follow 수는 자주 변하므로 30초
        CONTENT_RANKING, defaultConfig.entryTtl(Duration.ofMinutes(2)) ,    //  Ranking도 자주 바뀌지만 실시간일 필요는 없다: 2분
        REVIEW_LIST, defaultConfig.entryTtl(Duration.ofSeconds(30))
    );

    ///  Redis CacheManager 만든다.
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware()
        .build();
  }

  private RedisCacheConfiguration redisCacheConfiguration(
      ObjectMapper objectMapper,
      Duration ttl
  ) {
    // 모든 클래스를 허용하지 말고, 우리 애플리케이션 패키지에 속한 클래스만 타입으로 인정
    // 외부에서 위조한 타입으로 역직렬화 할 수 있기때문
    BasicPolymorphicTypeValidator typeValidator =
            BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType("com.team6.moduply.")
                    .allowIfSubType(ArrayList.class)
                    .allowIfSubType(UUID.class)
                    .allowIfSubType(LocalDateTime.class)
                    // DTO의 createdAt/updatedAt에 쓰이는 Instant 타입 캐시 역직렬화를 허용한다.
                    .allowIfSubType(Instant.class)
                    .allowIfSubType(BigDecimal.class)
                    .build();

    ObjectMapper cacheObjectMapper = objectMapper.copy();

    cacheObjectMapper.activateDefaultTyping(
        typeValidator,
        ObjectMapper.DefaultTyping.EVERYTHING,      // Redis에 객체를 저장할 때 실제 클래스 정보를 JSON에 함께 넣는다.
        JsonTypeInfo.As.PROPERTY
    );

    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(ttl)                  // 캐시 유지 시간
        .prefixCacheNameWith("moduply:cache:v3:")
        .disableCachingNullValues()     // null은 캐싱 x
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())     // Redis Key를 문자열로 저장
        )
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(cacheObjectMapper)
            )
        );
  }

  /*
   * 기존 Caffeine L1 캐시 설정.
   * 분산 환경에서는 인스턴스별 로컬 캐시가 서로 공유되지 않으므로 Redis CacheManager로 전환한다.
   *
   * @Bean
   * public CacheManager cacheManager() {
   *   CaffeineCacheManager cacheManager = new CaffeineCacheManager();
   *   cacheManager.setAllowNullValues(false);
   *   cacheManager.registerCustomCache(USER_SUMMARY, cache(Duration.ofMinutes(5), 10_000));
   *   cacheManager.registerCustomCache(CONTENT_LIST, cache(Duration.ofSeconds(30), 5_000));
   *   cacheManager.registerCustomCache(CONTENT_DETAIL, cache(Duration.ofMinutes(5), 5_000));
   *   cacheManager.registerCustomCache(CONTENT_TAGS, cache(Duration.ofMinutes(10), 10_000));
   *   cacheManager.registerCustomCache(PROFILE_IMAGE_URL, cache(Duration.ofMinutes(5), 20_000));
   *   cacheManager.registerCustomCache(PLAYLIST_DETAIL, cache(Duration.ofMinutes(5), 5_000));
   *   cacheManager.registerCustomCache(FOLLOW_COUNT, cache(Duration.ofSeconds(30), 20_000));
   *   cacheManager.registerCustomCache(CONTENT_RANKING, cache(Duration.ofMinutes(2), 1_000));
   *   return cacheManager;
   * }
   */
}
