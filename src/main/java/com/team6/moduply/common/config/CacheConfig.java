package com.team6.moduply.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  public static final String USER_SUMMARY = "userSummary";
  public static final String CONTENT_LIST = "contentList";
  public static final String CONTENT_DETAIL = "contentDetail";
  public static final String PROFILE_IMAGE_URL = "profileImageUrl";
  public static final String PLAYLIST_DETAIL = "playlistDetail";
  public static final String FOLLOW_COUNT = "followCount";
  public static final String CONTENT_RANKING = "contentRanking";

  /** 전체 캐시 관리자 등록

   Spring Cache 흐름
   @Cacheable -> CacheManager -> Caffeine Cache -> JVM Memory
   **/

  @Bean
  public CacheManager cacheManager() {
    // Spring과 Caffeine 연결해주는 객체
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    // null을 캐싱하지 않는다.
    // 나중에 null값이던게 data가 생성되면 캐시에 null이 남아있어서 새 데이터 못읽음 이슈대비.
    cacheManager.setAllowNullValues(false);

    // 캐시이름: userSummary
    // TTL(만료시간): 5분
    // 최대 저장 개수 10,000개
    cacheManager.registerCustomCache(
        USER_SUMMARY,
        cache(Duration.ofMinutes(5), 10_000)
    );
    // 캐시이름: contentList
    cacheManager.registerCustomCache(
        CONTENT_LIST,
        cache(Duration.ofSeconds(30), 5_000)
    );
    cacheManager.registerCustomCache(
        CONTENT_DETAIL,
        cache(Duration.ofMinutes(10), 5_000)
    );

    // Presigned URL 만료 문제 고려해서 TTL 5분으로 설정.
    cacheManager.registerCustomCache(
        PROFILE_IMAGE_URL,
        cache(Duration.ofMinutes(5), 20_000)
    );
    cacheManager.registerCustomCache(
        PLAYLIST_DETAIL,
        cache(Duration.ofMinutes(5), 5_000)
    );

    // 팔로우 수는 자주 바뀌므로 TTL 짧게: 30초
    cacheManager.registerCustomCache(
        FOLLOW_COUNT,
        cache(Duration.ofSeconds(30), 20_000)
    );
    cacheManager.registerCustomCache(
        CONTENT_RANKING,
        cache(Duration.ofMinutes(2), 1_000)
    );

    return cacheManager;
  }

  private Cache<Object, Object> cache(
      Duration ttl,
      long maximumSize
  ) {
    return Caffeine.newBuilder()
        // 캐시 저장된수 TTL만큼 경과하면 자동제거
        .expireAfterWrite(ttl)
        .maximumSize(maximumSize)
        // 캐시 통계 수집
        // cache.status()로 hitCount, missCount로 통계 확인가능.
        .recordStats()
        .build();
  }
}
