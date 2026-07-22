package com.team6.moduply.content.service;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.config.CacheConfig;
import com.team6.moduply.content.dto.ContentDetailCacheDto;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentDetailCacheService {

  private static final String DEFAULT_THUMBNAIL_URL = "/placeholder-movie.png";

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final BinaryContentService binaryContentService;

  @Cacheable(cacheNames = CacheConfig.CONTENT_DETAIL, key = "#contentId", sync = true)
  @Transactional(readOnly = true)
  public ContentDetailCacheDto find(UUID contentId) {
    Content content = contentRepository.findByIdWithContentImg(contentId)
        .orElseThrow(() -> {
          log.warn("콘텐츠 단건 캐시 조회 실패: 콘텐츠 없음. contentId={}", contentId);
          return new ContentException(
              ContentErrorCode.CONTENT_NOT_FOUND,
              Map.of("contentId", contentId)
          );
        });

    String thumbnailUrl = content.getContentImg() == null
        ? DEFAULT_THUMBNAIL_URL
        : binaryContentService.generateUrl(content.getContentImg());

    return new ContentDetailCacheDto(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        thumbnailUrl,
        contentTagRepository.findTagNamesByContentId(contentId),
        content.getAverageRating(),
        content.getReviewCount()
    );
  }
}
