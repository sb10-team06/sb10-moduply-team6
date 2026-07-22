package com.team6.moduply.content.search.service;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.ContentTagRepository.ContentTagNameProjection;
import com.team6.moduply.content.search.document.ContentSearchDocument;
import com.team6.moduply.content.search.mapper.ContentSearchDocumentMapper;
import com.team6.moduply.content.search.repository.ContentSearchRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentSearchIndexService {

  private static final int REBUILD_PAGE_SIZE = 500;

  private final ContentSearchRepository contentSearchRepository;
  private final ContentSearchDocumentMapper contentSearchDocumentMapper;
  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final ElasticsearchOperations elasticsearchOperations;

  public void index(Content content, List<String> tagNames) {
    runAfterCommit(() -> saveDocument(content, tagNames));
  }

  public void indexAll(List<Content> contents, Map<UUID, List<String>> tagNamesByContentId) {
    if (contents.isEmpty()) {
      return;
    }

    runAfterCommit(() -> saveDocuments(contents, tagNamesByContentId));
  }

  public void delete(UUID contentId) {
    runAfterCommit(() -> deleteDocument(contentId));
  }

  public void rebuildAllIfEmpty() {
    long contentCount = contentRepository.count();
    if (hasIndexedDocuments()) {
      log.info("콘텐츠 검색 인덱스가 이미 존재하여 재색인을 건너뜁니다. contentCount={}", contentCount);
      return;
    }

    if (contentCount == 0) {
      log.info("콘텐츠가 없어 검색 인덱스 재색인을 건너뜁니다.");
      return;
    }

    int indexedCount = rebuildAll();
    log.info("콘텐츠 검색 인덱스 자동 재색인 완료: contentCount={}, indexedCount={}",
        contentCount, indexedCount);
  }

  private void runAfterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        action.run();
      }
    });
  }

  private void saveDocument(Content content, List<String> tagNames) {
    try {
      contentSearchRepository.save(contentSearchDocumentMapper.toDocument(content, tagNames));
      log.debug("콘텐츠 검색 인덱스 저장 완료. contentId={}", content.getId());
    } catch (RuntimeException e) {
      log.warn("콘텐츠 검색 인덱스 저장 실패. contentId={}", content.getId(), e);
    }
  }

  private void saveDocuments(List<Content> contents, Map<UUID, List<String>> tagNamesByContentId) {
    try {
      contentSearchRepository.saveAll(toDocuments(contents, tagNamesByContentId));
      log.debug("콘텐츠 검색 인덱스 일괄 저장 완료. contentCount={}", contents.size());
    } catch (RuntimeException e) {
      log.warn("콘텐츠 검색 인덱스 일괄 저장 실패. contentCount={}", contents.size(), e);
    }
  }

  private int rebuildAll() {
    int pageNumber = 0;
    int indexedCount = 0;
    Page<Content> page;

    do {
      page = contentRepository.findAll(
          PageRequest.of(pageNumber, REBUILD_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"))
      );
      List<Content> contents = page.getContent();
      Map<UUID, List<String>> tagNamesByContentId = getTagNamesGroupedByContentId(contents);
      contentSearchRepository.saveAll(toDocuments(contents, tagNamesByContentId));
      indexedCount += contents.size();
      pageNumber++;
    } while (page.hasNext());

    return indexedCount;
  }

  private List<ContentSearchDocument> toDocuments(
      List<Content> contents,
      Map<UUID, List<String>> tagNamesByContentId
  ) {
    return contents.stream()
        .map(content -> contentSearchDocumentMapper.toDocument(
            content,
            tagNamesByContentId.getOrDefault(content.getId(), List.of())
        ))
        .toList();
  }

  private void deleteDocument(UUID contentId) {
    try {
      contentSearchRepository.deleteById(contentId.toString());
      log.debug("콘텐츠 검색 인덱스 삭제 완료. contentId={}", contentId);
    } catch (RuntimeException e) {
      log.warn("콘텐츠 검색 인덱스 삭제 실패. contentId={}", contentId, e);
    }
  }

  private boolean hasIndexedDocuments() {
    IndexOperations indexOperations = elasticsearchOperations.indexOps(ContentSearchDocument.class);
    if (!indexOperations.exists()) {
      indexOperations.createWithMapping();
      log.info("콘텐츠 검색 인덱스가 없어 새로 생성했습니다.");
      return false;
    }

    return contentSearchRepository.count() > 0;
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
