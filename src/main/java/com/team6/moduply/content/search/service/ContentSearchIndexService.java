package com.team6.moduply.content.search.service;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.search.document.ContentSearchDocument;
import com.team6.moduply.content.search.mapper.ContentSearchDocumentMapper;
import com.team6.moduply.content.search.repository.ContentSearchRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentSearchIndexService {

  private final ContentSearchRepository contentSearchRepository;
  private final ContentSearchDocumentMapper contentSearchDocumentMapper;

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
    } catch (RuntimeException e) {
      log.warn("콘텐츠 검색 인덱스 저장 실패. contentId={}", content.getId(), e);
    }
  }

  private void saveDocuments(List<Content> contents, Map<UUID, List<String>> tagNamesByContentId) {
    try {
      List<ContentSearchDocument> documents = contents.stream()
          .map(content -> contentSearchDocumentMapper.toDocument(
              content,
              tagNamesByContentId.getOrDefault(content.getId(), List.of())
          ))
          .toList();
      contentSearchRepository.saveAll(documents);
    } catch (RuntimeException e) {
      log.warn("콘텐츠 검색 인덱스 일괄 저장 실패. contentCount={}", contents.size(), e);
    }
  }

  private void deleteDocument(UUID contentId) {
    try {
      contentSearchRepository.deleteById(contentId.toString());
    } catch (RuntimeException e) {
      log.warn("콘텐츠 검색 인덱스 삭제 실패. contentId={}", contentId, e);
    }
  }
}
