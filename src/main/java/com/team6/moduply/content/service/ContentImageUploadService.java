package com.team6.moduply.content.service;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.event.BinaryContentDeletedEvent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.common.config.CacheConfig;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.repository.ContentRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentImageUploadService {

  private final BinaryContentRepository binaryContentRepository;
  private final ContentRepository contentRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(
      UUID contentId,
      UUID binaryContentId,
      String thumbnailUrl,
      UUID oldBinaryContentId,
      String oldStorageKey
  ) {
    BinaryContent binaryContent = binaryContentRepository.findById(binaryContentId)
        .orElseThrow(() -> new BinaryContentException(
            BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND,
            Map.of("binaryContentId", binaryContentId)
        ));
    Content content = contentRepository.findByIdWithContentImgForUpdate(contentId)
        .orElseThrow(() -> new IllegalStateException("이미지 URL을 저장할 콘텐츠가 없습니다."));

    BinaryContent currentImage = content.getContentImg();
    boolean currentImageMatches = oldBinaryContentId == null
        ? currentImage == null
        : currentImage != null && currentImage.getId().equals(oldBinaryContentId);
    if (!currentImageMatches) {
      throw new IllegalStateException("콘텐츠 이미지가 업로드 시작 시점과 다릅니다.");
    }

    content.updateContentImage(binaryContent, thumbnailUrl);
    binaryContent.success();

    if (oldBinaryContentId != null) {
      eventPublisher.publishEvent(
          new BinaryContentDeletedEvent(oldBinaryContentId, oldStorageKey)
      );
    }
  }

  @Caching(evict = {
      @CacheEvict(cacheNames = CacheConfig.CONTENT_LIST, allEntries = true),
      @CacheEvict(cacheNames = CacheConfig.CONTENT_DETAIL, key = "#contentId"),
      @CacheEvict(cacheNames = CacheConfig.PLAYLIST_DETAIL, allEntries = true),
      @CacheEvict(cacheNames = CacheConfig.IMAGE_URL, key = "#binaryContentId")
  })
  public void evictCaches(UUID contentId, UUID binaryContentId) {
    // 캐시 annotation 적용을 위한 진입점이다.
  }
}
