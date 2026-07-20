package com.team6.moduply.content.service;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.dto.ContentFindAllRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
  private final ReviewQDSLRepository reviewQDSLRepository;
  private final WatchingSessionRepository watchingSessionRepository;
  private final ContentSearchIndexService contentSearchIndexService;

  @PreAuthorize("hasRole('ADMIN')")
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

    List<Content> contents = contentRepository.findAllByCursor(
        request.typeEqual(),
        request.keywordLike(),
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
    List<ContentDto> data = pageContents.stream()
        .map(content -> toDto(
            content,
            tagNamesByContentId.getOrDefault(content.getId(), List.of())
        ))
        .toList();
    Content lastContent = data.isEmpty() ? null : pageContents.get(pageContents.size() - 1);
    boolean isFirstPage = request.cursor() == null && request.idAfter() == null;
    long totalCount = isFirstPage && !hasNext
        ? pageContents.size()
        : contentRepository.countContents(
            request.typeEqual(),
            request.keywordLike(),
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

    List<String> tagNames = contentTagRepository.findTagNamesByContentId(contentId);

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
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
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

  private ContentDto toDto(Content content, List<String> tagNames) {
    String thumbnailUrl = content.getContentImg() == null
        ? DEFAULT_THUMBNAIL_URL
        : binaryContentService.generateUrl(content.getContentImg());
    ContentDto contentDto = contentMapper.toDto(content, thumbnailUrl, tagNames);
    return withCurrentWatcherCount(contentDto, content.getId());
  }

  private ContentDto withCurrentWatcherCount(ContentDto contentDto, UUID contentId) {
    long watcherCount = watchingSessionRepository.countByContentId(contentId);
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

  private String extractCursor(Content content, ContentSortBy sortBy) {
    return switch (sortBy) {
      case createdAt -> content.getCreatedAt().toString();
      case watcherCount -> String.valueOf(content.getWatcherCount());
      case rate -> content.getAverageRating().toPlainString();
    };
  }

}
