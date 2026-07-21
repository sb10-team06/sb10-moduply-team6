package com.team6.moduply.content.service;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.config.CacheConfig;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.dto.ContentFindAllRequest;
import com.team6.moduply.content.dto.ContentListCacheItemDto;
import com.team6.moduply.content.dto.ContentListCachePageDto;
import com.team6.moduply.content.dto.ContentUpdateRequest;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.mapper.ContentMapper;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.ContentTagRepository.ContentTagNameProjection;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.TagRepository;
import com.team6.moduply.content.search.service.ContentSearchIndexService;
import com.team6.moduply.content.search.service.ContentSearchService;
import com.team6.moduply.content.search.service.ContentSearchService.ContentSearchResult;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository.ReviewStats;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

  private static final String DEFAULT_THUMBNAIL_URL = "/placeholder-movie.png";

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentTagRepository contentTagRepository;
  private final ContentMapper contentMapper;
  private final BinaryContentService binaryContentService;
  private final ContentListCacheService contentListCacheService;
  private final ContentTagCacheService contentTagCacheService;
  private final ReviewQDSLRepository reviewQDSLRepository;
  private final WatchingSessionRepository watchingSessionRepository;
  private final ContentSearchIndexService contentSearchIndexService;
  private final ContentSearchService contentSearchService;

  @PreAuthorize("hasRole('ADMIN')")
  @CacheEvict(cacheNames = CacheConfig.CONTENT_LIST, allEntries = true)
  @Transactional
  public ContentDto create(
      ContentCreateRequest request,
      MultipartFile thumbnail
  ) {
    log.debug("콘텐츠 생성 처리 시작: type={}, title={}", request.type(), request.title());

    Content content = new Content(
        null,
        null,
        request.type(),
        request.title(),
        request.description()
    );
    Content savedContent = contentRepository.save(content);
    BinaryContent contentImg = createContentImage(savedContent, thumbnail);
    savedContent.updateContentImg(contentImg);
    String thumbnailUrl = binaryContentService.generateUrl(contentImg);

    List<String> tagNames = normalizeTagsOrDefault(request.tags(), request.type());

    List<Tag> tags = getOrCreateTags(tagNames);

    List<ContentTag> contentTags = tags.stream()
        .map(tag -> new ContentTag(savedContent, tag))
        .toList();

    if (!contentTags.isEmpty()) {
      contentTagRepository.saveAll(contentTags);
    }

    contentSearchIndexService.index(savedContent, tagNames);

    ContentDto response = withCurrentWatcherCount(
        contentMapper.toDto(savedContent, thumbnailUrl, tagNames),
        savedContent.getId()
    );

    log.debug("콘텐츠 생성 처리 완료: contentId={}", savedContent.getId());

    return response;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Caching(evict = {
      @CacheEvict(cacheNames = CacheConfig.CONTENT_LIST, allEntries = true),
      @CacheEvict(cacheNames = CacheConfig.CONTENT_TAGS, key = "#contentId")
  })
  @Transactional
  public ContentDto update(
      UUID contentId,
      ContentUpdateRequest request,
      MultipartFile thumbnail
  ) {
    log.debug("콘텐츠 수정 처리 시작: contentId={}", contentId);

    Content content = findContent(contentId);
    content.update(
        request.title(),
        request.description()
    );

    if (thumbnail != null && !thumbnail.isEmpty()) {
      BinaryContent contentImg = createContentImage(content, thumbnail);
      content.updateContentImg(contentImg);
    }

    List<String> tagNames = updateTagsIfPresent(content, request.tags());
    contentSearchIndexService.index(content, tagNames);

    ContentDto response = toDto(content, tagNames);

    log.debug("콘텐츠 수정 처리 완료: contentId={}", contentId);

    return response;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Caching(evict = {
      @CacheEvict(cacheNames = CacheConfig.CONTENT_LIST, allEntries = true),
      @CacheEvict(cacheNames = CacheConfig.CONTENT_TAGS, key = "#contentId")
  })
  @Transactional
  public void delete(UUID contentId) {
    log.debug("콘텐츠 삭제 처리 시작: contentId={}", contentId);

    Content content = findContent(contentId);
    BinaryContent contentImg = content.getContentImg();

    contentTagRepository.deleteAllByContentId(contentId);

    if (contentImg != null) {
      content.updateContentImg(null);
      contentRepository.flush();
    }

    contentRepository.delete(content);
    contentSearchIndexService.delete(contentId);

    if (contentImg != null) {
      binaryContentService.delete(contentImg.getId());
    }

    log.debug("콘텐츠 삭제 처리 완료: contentId={}", contentId);
  }

  @Transactional(readOnly = true)
  public CursorResponse<ContentDto> findAll(ContentFindAllRequest request) {
    List<String> normalizedTagsIn = normalizeTags(request.tagsIn());

    log.debug(
        "콘텐츠 목록 조회 처리 시작: typeEqual={}, keywordLike={}, tagsIn={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
        request.typeEqual(),
        request.keywordLike(),
        normalizedTagsIn,
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortBy(),
        request.sortDirection()
    );

    if (StringUtils.hasText(request.keywordLike())) {
      return findAllBySearchIndex(request, normalizedTagsIn);
    }

    /// 최신순(createdAt) 정렬이라면 contentListCacheService.findCreatedAtPage 호출
    if (request.sortBy() == ContentSortBy.createdAt) {
      ContentListCachePageDto cachedPage = contentListCacheService.findCreatedAtPage(
          request.typeEqual(),
          request.keywordLike(),
          normalizedTagsIn,
          request.cursor(),
          request.idAfter(),
          request.limit(),
          request.sortBy(),
          request.sortDirection()
      );
      /// 평균 평점, 리뷰 수, 현재 시청자 수 를 합쳐서 응답.
      CursorResponse<ContentDto> response = toContentResponseWithDynamicValues(cachedPage);
      log.debug("콘텐츠 목록 조회 처리 완료: count={}, hasNext={}", response.data().size(), response.hasNext());
      return response;
    }

    List<Content> contents = contentRepository.findAllByCursor(
        request.typeEqual(),
        null,
        normalizedTagsIn,
        request.cursor(),
        request.idAfter(),
        request.limit() + 1,
        request.sortBy(),
        request.sortDirection()
    );
    boolean hasNext = contents.size() > request.limit();
    List<Content> pageContents = hasNext ? contents.subList(0, request.limit()) : contents;
    Map<UUID, List<String>> tagNamesByContentId = getTagNamesGroupedByContentId(pageContents);
    // (콘텐츠: 시청자 수)
    Map<UUID, Long> watcherCountsByContentId = getWatcherCountsByContentId(pageContents);
    List<ContentDto> data = pageContents.stream()
        .map(content -> toDto(
            content,
            tagNamesByContentId.getOrDefault(content.getId(), List.of()),
            // 시청자 수 꺼내기
            watcherCountsByContentId.getOrDefault(content.getId(), 0L)
        ))
        .toList();
    Content lastContent = data.isEmpty() ? null : pageContents.get(pageContents.size() - 1);
    boolean isFirstPage = request.cursor() == null && request.idAfter() == null;
    long totalCount = isFirstPage && !hasNext
        ? pageContents.size()
        : contentRepository.countContents(
            request.typeEqual(),
            null,
            normalizedTagsIn
        );

    CursorResponse<ContentDto> response = new CursorResponse<>(
        data,
        hasNext ? extractCursor(lastContent, request.sortBy()) : null,
        hasNext ? lastContent.getId() : null,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );

    log.debug("콘텐츠 목록 조회 처리 완료: count={}, hasNext={}", data.size(), hasNext);

    return response;
  }

  private CursorResponse<ContentDto> findAllBySearchIndex(
      ContentFindAllRequest request,
      List<String> normalizedTagsIn
  ) {
    ContentSortBy searchSortBy = toSearchSortBy(request.sortBy());
    ContentSearchResult searchResult = contentSearchService.search(
        request.typeEqual(),
        request.keywordLike(),
        normalizedTagsIn,
        request.cursor(),
        request.idAfter(),
        request.limit(),
        searchSortBy,
        request.sortDirection()
    );

    List<Content> pageContents = sortBySearchResultOrder(
        contentRepository.findAllByIdIn(searchResult.contentIds()),
        searchResult.contentIds()
    );
    Map<UUID, List<String>> tagNamesByContentId = getTagNamesGroupedByContentId(pageContents);
    List<ContentDto> data = pageContents.stream()
        .map(content -> toDto(
            content,
            tagNamesByContentId.getOrDefault(content.getId(), List.of())
        ))
        .toList();

    return new CursorResponse<>(
        data,
        searchResult.hasNext() ? searchResult.nextCursor() : null,
        searchResult.hasNext() ? searchResult.nextIdAfter() : null,
        searchResult.hasNext(),
        searchResult.totalCount(),
        searchSortBy.name(),
        request.sortDirection()
    );
  }

  @Transactional(readOnly = true)
  public ContentDto find(UUID contentId) {
    log.debug("콘텐츠 단건 조회 처리 시작: contentId={}", contentId);

    Content content = contentRepository.findByIdWithContentImg(contentId)
        .orElseThrow(() -> {
          log.warn("콘텐츠 단건 조회 실패: 콘텐츠 없음. contentId={}", contentId);
          return new ContentException(
              ContentErrorCode.CONTENT_NOT_FOUND,
              Map.of("contentId", contentId)
          );
        });

    List<String> tagNames = contentTagCacheService.findTagNamesByContentId(contentId);

    ContentDto response = toDto(content, tagNames);

    log.debug("콘텐츠 단건 조회 처리 완료: contentId={}", contentId);

    return response;
  }

  @Transactional(readOnly = true)
  public void validateExists(UUID contentId) {
    if (!contentRepository.existsById(contentId)) {
      throw new ContentException(ContentErrorCode.CONTENT_NOT_FOUND,
          Map.of("contentId", contentId));
    }
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CONTENT_LIST, allEntries = true)
  public void refreshReviewStats(UUID contentId) {
    Content content = findContentById(contentId);
    ReviewStats stats = reviewQDSLRepository.calculateStatsByContentId(contentId);

    content.updateReviewStats(
        BigDecimal.valueOf(stats.averageRating()).setScale(2, RoundingMode.HALF_UP),
        Math.toIntExact(stats.reviewCount())
    );

    contentSearchIndexService.index(
        content,
        contentTagRepository.findTagNamesByContentId(contentId)
    );
  }

  private BinaryContent createContentImage(Content content, MultipartFile thumbnail) {
    try {
      return binaryContentService.createContentImage(content.getId(), thumbnail,
          content.getContentImg());
    } catch (IOException e) {
      throw new BinaryContentException(
          BinaryContentErrorCode.FILE_READ_FAILED,
          Map.of("fileName", thumbnail == null ? "null" : thumbnail.getOriginalFilename()),
          e
      );
    }
  }

  private Content findContent(UUID contentId) {
    return contentRepository.findByIdWithContentImg(contentId)
        .orElseThrow(() -> {
          log.warn("콘텐츠 조회 실패: 콘텐츠 없음. contentId={}", contentId);
          return new ContentException(
              ContentErrorCode.CONTENT_NOT_FOUND,
              Map.of("contentId", contentId)
          );
        });
  }

  private Content findContentById(UUID contentId) {
    return contentRepository.findById(contentId)
        .orElseThrow(() -> {
          log.warn("콘텐츠 조회 실패: 콘텐츠 없음. contentId={}", contentId);
          return new ContentException(
              ContentErrorCode.CONTENT_NOT_FOUND,
              Map.of("contentId", contentId)
          );
        });
  }

  private List<String> normalizeTags(List<String> tags) {
    if (tags == null) {
      return List.of();
    }

    return tags.stream()
        .map(String::trim)
        .distinct()
        .sorted()
        .toList();
  }

  private List<String> normalizeTagsOrDefault(List<String> tags, ContentType type) {
    List<String> normalizedTags = normalizeTags(tags);
    if (!normalizedTags.isEmpty()) {
      return normalizedTags;
    }

    return List.of(type.name());
  }

  private List<Tag> getOrCreateTags(List<String> tagNames) {
    if (tagNames.isEmpty()) {
      return List.of();
    }

    Map<String, Tag> existingTags = tagRepository.findAllByTagNameIn(tagNames).stream()
        .collect(Collectors.toMap(Tag::getTagName, Function.identity()));

    Set<String> existingTagNames = existingTags.keySet();
    List<String> newTagNames = tagNames.stream()
        .filter(tagName -> !existingTagNames.contains(tagName))
        .toList();

    if (!newTagNames.isEmpty()) {
      newTagNames.forEach(tagName -> tagRepository.insertIgnore(UUID.randomUUID(), tagName));
      existingTags = tagRepository.findAllByTagNameIn(tagNames).stream()
          .collect(Collectors.toMap(Tag::getTagName, Function.identity()));
    }

    return tagNames.stream()
        .map(existingTags::get)
        .toList();
  }

  private List<String> updateTagsIfPresent(Content content, List<String> tags) {
    if (tags == null) {
      return contentTagRepository.findTagNamesByContentId(content.getId());
    }

    List<String> tagNames = normalizeTags(tags);
    List<String> existingTagNames = contentTagRepository.findTagNamesByContentId(content.getId());

    if (!tagNames.isEmpty()
        && new LinkedHashSet<>(existingTagNames).equals(new LinkedHashSet<>(tagNames))) {
      return existingTagNames;
    }

    contentTagRepository.deleteAllByContentId(content.getId());

    List<ContentTag> contentTags = getOrCreateTags(tagNames).stream()
        .map(tag -> new ContentTag(content, tag))
        .toList();

    if (!contentTags.isEmpty()) {
      contentTagRepository.saveAll(contentTags);
    }

    return tagNames;
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

  private List<Content> sortBySearchResultOrder(List<Content> contents, List<UUID> orderedIds) {
    Map<UUID, Content> contentById = contents.stream()
        .collect(Collectors.toMap(Content::getId, Function.identity()));

    return orderedIds.stream()
        .map(contentById::get)
        .filter(content -> content != null)
        .toList();
  }

  private ContentSortBy toSearchSortBy(ContentSortBy sortBy) {
    if (sortBy == ContentSortBy.watcherCount) {
      return ContentSortBy.createdAt;
    }

    return sortBy;
  }

  private ContentDto toDto(Content content, List<String> tagNames) {
    long watcherCount = watchingSessionRepository.countByContentId(content.getId());
    return toDto(content, tagNames, watcherCount);
  }

  private ContentDto toDto(Content content, List<String> tagNames, long watcherCount) {
    String thumbnailUrl = content.getContentImg() == null
        ? DEFAULT_THUMBNAIL_URL
        : binaryContentService.generateUrl(content.getContentImg());
    ContentDto contentDto = contentMapper.toDto(content, thumbnailUrl, tagNames);
    return withWatcherCount(contentDto, watcherCount);
  }

  private ContentDto withCurrentWatcherCount(ContentDto contentDto, UUID contentId) {
    long watcherCount = watchingSessionRepository.countByContentId(contentId);
    return withWatcherCount(contentDto, watcherCount);
  }

  private ContentDto withWatcherCount(ContentDto contentDto, long watcherCount) {
    return new ContentDto(
        contentDto.id(),
        contentDto.type(),
        contentDto.title(),
        contentDto.description(),
        contentDto.thumbnailUrl(),
        contentDto.tags(),
        contentDto.averageRating(),
        contentDto.reviewCount(),
        watcherCount
    );
  }

  private Map<UUID, Long> getWatcherCountsByContentId(List<Content> contents) {
    if (contents.isEmpty()) {
      return Map.of();
    }

    List<UUID> contentIds = contents.stream()
        .map(Content::getId)
        .toList();
    return watchingSessionRepository.countByContentIds(contentIds);
  }

  private CursorResponse<ContentDto> toContentResponseWithDynamicValues(
      ContentListCachePageDto cachedPage
  ) {
    List<UUID> contentIds = cachedPage.data().stream()
        .map(ContentListCacheItemDto::id)
        .toList();
    if (contentIds.isEmpty()) {
      return new CursorResponse<>(
          List.of(),
          cachedPage.nextCursor(),
          cachedPage.nextIdAfter(),
          cachedPage.hasNext(),
          cachedPage.totalCount(),
          cachedPage.sortBy(),
          cachedPage.sortDirection()
      );
    }

    Map<UUID, Long> watcherCountsByContentId = watchingSessionRepository.countByContentIds(contentIds);
    List<ContentDto> data = cachedPage.data().stream()
        .map(item -> toDto(item, watcherCountsByContentId))
        .toList();

    return new CursorResponse<>(
        data,
        cachedPage.nextCursor(),
        cachedPage.nextIdAfter(),
        cachedPage.hasNext(),
        cachedPage.totalCount(),
        cachedPage.sortBy(),
        cachedPage.sortDirection()
    );
  }

  private ContentDto toDto(
      ContentListCacheItemDto item,
      Map<UUID, Long> watcherCountsByContentId
  ) {
    return new ContentDto(
        item.id(),
        item.type(),
        item.title(),
        item.description(),
        item.thumbnailUrl(),
        item.tags(),
        item.averageRating() != null ? item.averageRating() : BigDecimal.ZERO,
        item.reviewCount(),
        watcherCountsByContentId.getOrDefault(item.id(), 0L)
    );
  }

  private String extractCursor(Content content, ContentSortBy sortBy) {
    return switch (sortBy) {
      case createdAt -> content.getCreatedAt().toString();
      case watcherCount -> String.valueOf(content.getWatcherCount());
      case rate -> content.getAverageRating().toPlainString();
    };
  }

}
