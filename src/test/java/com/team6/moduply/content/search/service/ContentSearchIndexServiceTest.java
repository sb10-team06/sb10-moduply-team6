package com.team6.moduply.content.search.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.search.document.ContentSearchDocument;
import com.team6.moduply.content.search.mapper.ContentSearchDocumentMapper;
import com.team6.moduply.content.search.repository.ContentSearchRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ContentSearchIndexServiceTest {

  @Mock
  private ContentSearchRepository contentSearchRepository;

  @Mock
  private ContentSearchDocumentMapper contentSearchDocumentMapper;

  @InjectMocks
  private ContentSearchIndexService contentSearchIndexService;

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("활성화된 트랜잭션 동기화가 없으면 즉시 콘텐츠 검색 인덱스를 저장한다.")
  void index_success_without_transaction_synchronization() {
    // Given
    Content content = createContent();
    ContentSearchDocument document = ContentSearchDocument.builder()
        .id(content.getId().toString())
        .build();
    given(contentSearchDocumentMapper.toDocument(content, List.of("SF"))).willReturn(document);

    // When
    contentSearchIndexService.index(content, List.of("SF"));

    // Then
    verify(contentSearchRepository).save(document);
  }

  @Test
  @DisplayName("활성화된 트랜잭션 동기화가 있으면 커밋 이후 콘텐츠 검색 인덱스를 저장한다.")
  void index_success_after_commit_when_transaction_synchronization_is_active() {
    // Given
    TransactionSynchronizationManager.initSynchronization();
    Content content = createContent();
    ContentSearchDocument document = ContentSearchDocument.builder()
        .id(content.getId().toString())
        .build();
    given(contentSearchDocumentMapper.toDocument(content, List.of("SF"))).willReturn(document);

    // When
    contentSearchIndexService.index(content, List.of("SF"));

    // Then
    verify(contentSearchRepository, never()).save(any());
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
    verify(contentSearchRepository).save(document);
  }

  @Test
  @DisplayName("여러 콘텐츠 검색 인덱스를 일괄 저장한다.")
  void indexAll_success() {
    // Given
    Content movie = createContent();
    Content sport = createContent();
    ContentSearchDocument movieDocument = ContentSearchDocument.builder()
        .id(movie.getId().toString())
        .build();
    ContentSearchDocument sportDocument = ContentSearchDocument.builder()
        .id(sport.getId().toString())
        .build();
    given(contentSearchDocumentMapper.toDocument(movie, List.of("movie")))
        .willReturn(movieDocument);
    given(contentSearchDocumentMapper.toDocument(sport, List.of()))
        .willReturn(sportDocument);

    // When
    contentSearchIndexService.indexAll(
        List.of(movie, sport),
        Map.of(movie.getId(), List.of("movie"))
    );

    // Then
    verify(contentSearchRepository).saveAll(List.of(movieDocument, sportDocument));
    verify(contentSearchDocumentMapper).toDocument(sport, List.of());
  }

  @Test
  @DisplayName("콘텐츠 목록이 비어 있으면 일괄 저장을 요청하지 않는다.")
  void indexAll_success_with_empty_contents() {
    // When
    contentSearchIndexService.indexAll(List.of(), Map.of());

    // Then
    verify(contentSearchRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("콘텐츠 검색 인덱스 저장 실패 시 예외를 전파하지 않는다.")
  void index_success_when_repository_save_fails() {
    // Given
    Content content = createContent();
    ContentSearchDocument document = ContentSearchDocument.builder()
        .id(content.getId().toString())
        .build();
    given(contentSearchDocumentMapper.toDocument(content, List.of("SF"))).willReturn(document);
    willThrow(new RuntimeException("elasticsearch unavailable"))
        .given(contentSearchRepository)
        .save(document);

    // When
    contentSearchIndexService.index(content, List.of("SF"));

    // Then
    verify(contentSearchRepository).save(document);
  }

  @Test
  @DisplayName("콘텐츠 삭제 시 검색 인덱스도 삭제한다.")
  void delete_success() {
    // Given
    UUID contentId = UUID.randomUUID();

    // When
    contentSearchIndexService.delete(contentId);

    // Then
    verify(contentSearchRepository).deleteById(contentId.toString());
  }

  private Content createContent() {
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 영화"
    );
    ReflectionTestUtils.setField(content, "id", UUID.randomUUID());
    return content;
  }
}
