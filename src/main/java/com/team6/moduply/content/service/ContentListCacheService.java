package com.team6.moduply.content.service;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.config.CacheConfig;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.dto.ContentListCacheItemDto;
import com.team6.moduply.content.dto.ContentListCachePageDto;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.ContentTagRepository.ContentTagNameProjection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentListCacheService {

  private static final String DEFAULT_THUMBNAIL_URL = "/placeholder-movie.png";

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final BinaryContentService binaryContentService;

  /// 캐시 키는 type, keyword, tags...를 모두 포함한다.
  @Cacheable(
      cacheNames = CacheConfig.CONTENT_LIST,
      key = "{#typeEqual, #keywordLike, #tagsIn, #cursor, #idAfter, #limit, #sortBy, #sortDirection}",
      sync = true
  )
  @Transactional(readOnly = true)
  public ContentListCachePageDto findCreatedAtPage(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn,
      String cursor,
      UUID idAfter,
      int limit,
      ContentSortBy sortBy,
      SortDirection sortDirection
  ) {
    /// 캐시가 없는 경우 콘텐츠 목록 조회
    List<Content> contents = contentRepository.findAllByCursor(
        typeEqual,
        keywordLike,
        tagsIn,
        cursor,
        idAfter,
        limit + 1,
        sortBy,
        sortDirection
    );
    boolean hasNext = contents.size() > limit;
    List<Content> pageContents = hasNext ? contents.subList(0, limit) : contents;
    Map<UUID, List<String>> tagNamesByContentId = getTagNamesGroupedByContentId(pageContents);
    List<ContentListCacheItemDto> data = pageContents.stream()
        .map(content -> toCacheItem(
            content,
            tagNamesByContentId.getOrDefault(content.getId(), List.of())
        ))
        .toList();
    Content lastContent = data.isEmpty() ? null : pageContents.get(pageContents.size() - 1);
    boolean isFirstPage = cursor == null && idAfter == null;
    long totalCount = isFirstPage && !hasNext
        ? pageContents.size()
        : contentRepository.countContents(typeEqual, keywordLike, tagsIn);
    /// id, title, description등 잘 변하지 않는 데이터 저장
    return new ContentListCachePageDto(
        data,
        hasNext ? lastContent.getCreatedAt().toString() : null,
        hasNext ? lastContent.getId() : null,
        hasNext,
        totalCount,
        sortBy.name(),
        sortDirection
    );
  }

  private ContentListCacheItemDto toCacheItem(Content content, List<String> tagNames) {
    String thumbnailUrl = content.getContentImg() == null
        ? DEFAULT_THUMBNAIL_URL
        : binaryContentService.generateUrl(content.getContentImg());
    return new ContentListCacheItemDto(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        thumbnailUrl,
        tagNames,
        content.getAverageRating(),
        content.getReviewCount()
    );
  }

  private Map<UUID, List<String>> getTagNamesGroupedByContentId(List<Content> contents) {
    List<UUID> contentIds = contents.stream()
        .map(Content::getId)
        .toList();

    if (contentIds.isEmpty()) {
      return Map.of();
    }

    return contentTagRepository.findTagNamesByContentIds(contentIds).stream()
        .collect(Collectors.groupingBy(
            ContentTagNameProjection::getContentId,
            Collectors.mapping(ContentTagNameProjection::getTagName, Collectors.toList())
        ));
  }
}
