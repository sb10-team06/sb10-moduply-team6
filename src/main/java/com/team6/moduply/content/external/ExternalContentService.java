package com.team6.moduply.content.external;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.TagRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalContentService {

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentTagRepository contentTagRepository;
  private final ExternalContentMapper externalContentMapper;
  private final ExternalImageClient externalImageClient;
  private final BinaryContentService binaryContentService;

  @Transactional
  public ExternalContentImportResult importTmdbMovies(List<TmdbMovieResponse> responses) {
    List<ExternalContentItem> items = emptyIfNull(responses).stream()
        .map(externalContentMapper::toItem)
        .toList();

    return saveNewContents(items);
  }

  @Transactional
  public ExternalContentImportResult importTmdbTvSeries(List<TmdbTvResponse> responses) {
    List<ExternalContentItem> items = emptyIfNull(responses).stream()
        .map(externalContentMapper::toItem)
        .toList();

    return saveNewContents(items);
  }

  @Transactional
  public ExternalContentImportResult importSportsEvents(List<SportsDbEventResponse> responses) {
    List<ExternalContentItem> items = emptyIfNull(responses).stream()
        .map(externalContentMapper::toItem)
        .toList();

    return saveNewContents(items);
  }

  private ExternalContentImportResult saveNewContents(List<ExternalContentItem> items) {
    if (items.isEmpty()) {
      return new ExternalContentImportResult(0, 0, 0, 0, 0);
    }

    Map<String, ExternalContentItem> itemByExternalApiId = items.stream()
        .collect(Collectors.toMap(
            ExternalContentItem::externalApiId,
            Function.identity(),
            (first, second) -> first,
            LinkedHashMap::new
        ));

    Set<String> existingExternalApiIds = contentRepository.findAllByExternalApiIdIn(
            itemByExternalApiId.keySet()
        ).stream()
        .map(Content::getExternalApiId)
        .collect(Collectors.toSet());

    List<ExternalContentItem> newItems = itemByExternalApiId.values().stream()
        .filter(item -> !existingExternalApiIds.contains(item.externalApiId()))
        .toList();

    List<Content> contents = newItems.stream()
        .map(item -> new Content(
            null,
            item.externalApiId(),
            item.type(),
            item.title(),
            item.description()
        ))
        .toList();

    List<Content> savedContents = contentRepository.saveAll(contents);
    ExternalImageSaveResult imageSaveResult = saveContentImages(savedContents, itemByExternalApiId);
    saveContentTags(savedContents, itemByExternalApiId);

    int skippedCount = items.size() - savedContents.size();
    log.debug(
        "외부 콘텐츠 저장 처리 완료: requestedCount={}, savedCount={}, skippedCount={}",
        items.size(),
        savedContents.size(),
        skippedCount
    );

    return new ExternalContentImportResult(
        items.size(),
        savedContents.size(),
        skippedCount,
        imageSaveResult.savedCount(),
        imageSaveResult.failedCount()
    );
  }

  private ExternalImageSaveResult saveContentImages(
      List<Content> contents,
      Map<String, ExternalContentItem> itemByExternalApiId
  ) {
    int imageSavedCount = 0;
    int imageFailedCount = 0;

    for (Content content : contents) {
      ExternalContentItem item = itemByExternalApiId.get(content.getExternalApiId());

      if (item == null || !StringUtils.hasText(item.thumbnailUrl())) {
        continue;
      }

      try {
        ExternalImageFile imageFile = externalImageClient.download(item.thumbnailUrl());
        BinaryContent contentImg = binaryContentService.createContentImage(
            content.getId(),
            imageFile.fileName(),
            imageFile.bytes(),
            imageFile.contentType(),
            null
        );
        content.updateContentImg(contentImg);
        imageSavedCount++;
      } catch (RuntimeException e) {
        imageFailedCount++;
        log.warn(
            "외부 콘텐츠 이미지 저장 실패. externalApiId={}, thumbnailUrl={}",
            content.getExternalApiId(),
            item.thumbnailUrl(),
            e
        );
      }
    }

    return new ExternalImageSaveResult(imageSavedCount, imageFailedCount);
  }

  private void saveContentTags(
      List<Content> contents,
      Map<String, ExternalContentItem> itemByExternalApiId
  ) {
    Map<String, Tag> tagByName = getOrCreateTagsByName(itemByExternalApiId.values());

    List<ContentTag> contentTags = contents.stream()
        .flatMap(content -> {
          ExternalContentItem item = itemByExternalApiId.get(content.getExternalApiId());
          return normalizeTags(item.tags()).stream()
              .map(tagByName::get)
              .map(tag -> new ContentTag(content, tag));
        })
        .toList();

    if (!contentTags.isEmpty()) {
      contentTagRepository.saveAll(contentTags);
    }
  }

  private Map<String, Tag> getOrCreateTagsByName(Collection<ExternalContentItem> items) {
    List<String> allTagNames = items.stream()
        .flatMap(item -> normalizeTags(item.tags()).stream())
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();

    return getOrCreateTags(allTagNames).stream()
        .collect(Collectors.toMap(Tag::getTagName, Function.identity()));
  }

  private List<Tag> getOrCreateTags(List<String> tagNames) {
    List<String> normalizedTagNames = normalizeTags(tagNames);

    if (normalizedTagNames.isEmpty()) {
      return List.of();
    }

    Map<String, Tag> existingTags = tagRepository.findAllByTagNameIn(normalizedTagNames).stream()
        .collect(Collectors.toMap(Tag::getTagName, Function.identity()));

    Set<String> existingTagNames = existingTags.keySet();
    List<String> newTagNames = normalizedTagNames.stream()
        .filter(tagName -> !existingTagNames.contains(tagName))
        .toList();

    if (!newTagNames.isEmpty()) {
      newTagNames.forEach(tagName -> tagRepository.insertIgnore(UUID.randomUUID(), tagName));
      existingTags = tagRepository.findAllByTagNameIn(normalizedTagNames).stream()
          .collect(Collectors.toMap(Tag::getTagName, Function.identity()));
    }

    return normalizedTagNames.stream()
        .map(existingTags::get)
        .toList();
  }

  private List<String> normalizeTags(List<String> tags) {
    if (tags == null) {
      return List.of();
    }

    return tags.stream()
        .map(String::trim)
        .filter(tag -> !tag.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();
  }

  private <T> List<T> emptyIfNull(Collection<T> values) {
    return values == null ? List.of() : List.copyOf(values);
  }

  private record ExternalImageSaveResult(
      int savedCount,
      int failedCount
  ) {
  }
}
