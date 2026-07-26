package com.team6.moduply.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
import com.team6.moduply.common.config.CacheConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig(BinaryContentServiceCacheTest.CacheTestConfig.class)
class BinaryContentServiceCacheTest {

  @Autowired
  private BinaryContentService service;
  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void clearCache() {
    cacheManager.getCache(CacheConfig.IMAGE_URL).clear();
  }

  @Test
  @DisplayName("DB 이미지 URL을 BinaryContent ID 기준으로 캐시한다.")
  void findUrl_success_cache_stored_url() {
    BinaryContent binaryContent = createBinaryContent();

    assertThat(service.findUrl(binaryContent, "https://bucket/image-v1.png"))
        .isEqualTo("https://bucket/image-v1.png");
    assertThat(service.findUrl(binaryContent, "https://bucket/image-v2.png"))
        .isEqualTo("https://bucket/image-v1.png");
  }

  @Test
  @DisplayName("DB 이미지 URL이 null이면 캐시에 저장하지 않는다.")
  void findUrl_success_skip_cache_when_url_is_null() {
    BinaryContent binaryContent = createBinaryContent();

    assertThat(service.findUrl(binaryContent, null)).isNull();
    assertThat(service.findUrl(binaryContent, "https://bucket/image.png"))
        .isEqualTo("https://bucket/image.png");
  }

  @Test
  @DisplayName("이미지 URL 캐시를 제거하면 다음 조회에서 변경된 DB URL을 반환한다.")
  void evictUrl_success_with_changed_url() {
    BinaryContent binaryContent = createBinaryContent();
    service.findUrl(binaryContent, "https://bucket/image-v1.png");

    service.evictUrl(binaryContent.getId());

    assertThat(service.findUrl(binaryContent, "https://bucket/image-v2.png"))
        .isEqualTo("https://bucket/image-v2.png");
  }

  private BinaryContent createBinaryContent() {
    BinaryContent binaryContent = BinaryContent.create(
        "image.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/image.png"
    );
    ReflectionTestUtils.setField(binaryContent, "id", UUID.randomUUID());
    return binaryContent;
  }

  @Configuration
  @EnableCaching
  static class CacheTestConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager(CacheConfig.IMAGE_URL);
    }

    @Bean
    BinaryContentService binaryContentService() {
      return new BinaryContentService(
          mock(BinaryContentStorage.class),
          mock(BinaryContentRepository.class),
          mock(ApplicationEventPublisher.class)
      );
    }
  }
}
