package com.team6.moduply.content.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.ContentTagRepository.ContentTagNameProjection;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ContentSearchIndexServiceTest {

  @Mock
  private ContentSearchRepository contentSearchRepository;

  @Mock
  private ContentSearchDocumentMapper contentSearchDocumentMapper;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  @Mock
  private ElasticsearchOperations elasticsearchOperations;

  @Mock
  private IndexOperations indexOperations;

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
  @DisplayName("여러 콘텐츠 검색 인덱스 저장 실패 시 예외를 전파하지 않는다.")
  void indexAll_success_when_repository_save_all_fails() {
    // Given
    Content content = createContent();
    ContentSearchDocument document = ContentSearchDocument.builder()
        .id(content.getId().toString())
        .build();
    given(contentSearchDocumentMapper.toDocument(content, List.of("SF"))).willReturn(document);
    willThrow(new RuntimeException("elasticsearch unavailable"))
        .given(contentSearchRepository)
        .saveAll(List.of(document));

    // When
    contentSearchIndexService.indexAll(List.of(content), Map.of(content.getId(), List.of("SF")));

    // Then
    verify(contentSearchRepository).saveAll(List.of(document));
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
  @DisplayName("DB 콘텐츠가 있고 검색 인덱스가 비어 있으면 전체 재색인한다.")
  void rebuildAllIfEmpty_success_when_index_is_empty() {
    // Given
    Content movie = createContent();
    Content sport = createContent();
    ContentSearchDocument movieDocument = ContentSearchDocument.builder()
        .id(movie.getId().toString())
        .build();
    ContentSearchDocument sportDocument = ContentSearchDocument.builder()
        .id(sport.getId().toString())
        .build();
    PageRequest pageRequest = PageRequest.of(
        0,
        500,
        Sort.by(Sort.Direction.ASC, "id")
    );

    given(contentRepository.count()).willReturn(2L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(false);
    given(indexOperations.createWithMapping()).willReturn(true);
    given(contentRepository.findAll(pageRequest))
        .willReturn(new PageImpl<>(List.of(movie, sport), pageRequest, 2));
    given(contentTagRepository.findTagNamesByContentIds(List.of(movie.getId(), sport.getId())))
        .willReturn(List.of(
            new TestContentTagNameProjection(movie.getId(), "movie"),
            new TestContentTagNameProjection(sport.getId(), "SportsDB"),
            new TestContentTagNameProjection(sport.getId(), "Soccer")
        ));
    given(contentSearchDocumentMapper.toDocument(movie, List.of("movie")))
        .willReturn(movieDocument);
    given(contentSearchDocumentMapper.toDocument(sport, List.of("SportsDB", "Soccer")))
        .willReturn(sportDocument);

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(indexOperations).createWithMapping();
    verify(contentSearchRepository).saveAll(List.of(movieDocument, sportDocument));
  }

  @Test
  @DisplayName("검색 인덱스가 존재하지만 문서가 없으면 전체 재색인한다.")
  void rebuildAllIfEmpty_success_when_existing_index_has_no_documents() {
    // Given
    Content content = createContent();
    ContentSearchDocument document = ContentSearchDocument.builder()
        .id(content.getId().toString())
        .build();
    PageRequest pageRequest = PageRequest.of(
        0,
        500,
        Sort.by(Sort.Direction.ASC, "id")
    );

    given(contentRepository.count()).willReturn(1L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(true);
    given(contentSearchRepository.count()).willReturn(0L);
    given(contentRepository.findAll(pageRequest))
        .willReturn(new PageImpl<>(List.of(content), pageRequest, 1));
    given(contentTagRepository.findTagNamesByContentIds(List.of(content.getId())))
        .willReturn(List.of(new TestContentTagNameProjection(content.getId(), "movie")));
    given(contentSearchDocumentMapper.toDocument(content, List.of("movie")))
        .willReturn(document);

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(indexOperations, never()).delete();
    verify(contentSearchRepository).saveAll(List.of(document));
  }

  @Test
  @DisplayName("전체 재색인 대상이 여러 페이지이면 모든 페이지를 색인한다.")
  void rebuildAllIfEmpty_success_with_multiple_pages() {
    // Given
    Content first = createContent();
    Content second = createContent();
    ContentSearchDocument firstDocument = ContentSearchDocument.builder()
        .id(first.getId().toString())
        .build();
    ContentSearchDocument secondDocument = ContentSearchDocument.builder()
        .id(second.getId().toString())
        .build();
    PageRequest firstPageRequest = PageRequest.of(
        0,
        500,
        Sort.by(Sort.Direction.ASC, "id")
    );
    PageRequest secondPageRequest = PageRequest.of(
        1,
        500,
        Sort.by(Sort.Direction.ASC, "id")
    );

    given(contentRepository.count()).willReturn(501L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(false);
    given(indexOperations.createWithMapping()).willReturn(true);
    given(contentRepository.findAll(firstPageRequest))
        .willReturn(new PageImpl<>(List.of(first), firstPageRequest, 501));
    given(contentRepository.findAll(secondPageRequest))
        .willReturn(new PageImpl<>(List.of(second), secondPageRequest, 501));
    given(contentTagRepository.findTagNamesByContentIds(List.of(first.getId())))
        .willReturn(List.of(new TestContentTagNameProjection(first.getId(), "movie")));
    given(contentTagRepository.findTagNamesByContentIds(List.of(second.getId())))
        .willReturn(List.of(new TestContentTagNameProjection(second.getId(), "tvSeries")));
    given(contentSearchDocumentMapper.toDocument(first, List.of("movie")))
        .willReturn(firstDocument);
    given(contentSearchDocumentMapper.toDocument(second, List.of("tvSeries")))
        .willReturn(secondDocument);

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(contentSearchRepository).saveAll(List.of(firstDocument));
    verify(contentSearchRepository).saveAll(List.of(secondDocument));
  }

  @Test
  @DisplayName("전체 재색인 중 페이지 콘텐츠가 비어 있으면 태그 조회 없이 빈 목록을 색인한다.")
  void rebuildAllIfEmpty_success_with_empty_rebuild_page() {
    // Given
    PageRequest pageRequest = PageRequest.of(
        0,
        500,
        Sort.by(Sort.Direction.ASC, "id")
    );

    given(contentRepository.count()).willReturn(1L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(false);
    given(indexOperations.createWithMapping()).willReturn(true);
    given(contentRepository.findAll(pageRequest))
        .willReturn(new PageImpl<>(List.of(), pageRequest, 0));

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(contentTagRepository, never()).findTagNamesByContentIds(anyList());
    verify(contentSearchRepository).saveAll(List.of());
  }

  @Test
  @DisplayName("검색 인덱스에 문서가 이미 있으면 전체 재색인을 건너뛴다.")
  void rebuildAllIfEmpty_skip_when_index_has_documents() {
    // Given
    given(contentRepository.count()).willReturn(10L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(true);
    given(contentSearchRepository.count()).willReturn(10L);

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(contentRepository, never()).findAll(any(PageRequest.class));
    verify(contentSearchRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("DB 콘텐츠 수와 검색 인덱스 문서 수가 다르면 인덱스를 재생성하고 전체 재색인한다.")
  void rebuildAllIfEmpty_success_recreate_index_when_document_count_is_mismatched() {
    // Given
    Content movie = createContent();
    ContentSearchDocument movieDocument = ContentSearchDocument.builder()
        .id(movie.getId().toString())
        .build();
    PageRequest pageRequest = PageRequest.of(
        0,
        500,
        Sort.by(Sort.Direction.ASC, "id")
    );

    given(contentRepository.count()).willReturn(1L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(true);
    given(contentSearchRepository.count()).willReturn(2L);
    given(indexOperations.delete()).willReturn(true);
    given(indexOperations.createWithMapping()).willReturn(true);
    given(contentRepository.findAll(pageRequest))
        .willReturn(new PageImpl<>(List.of(movie), pageRequest, 1));
    given(contentTagRepository.findTagNamesByContentIds(List.of(movie.getId())))
        .willReturn(List.of(new TestContentTagNameProjection(movie.getId(), "movie")));
    given(contentSearchDocumentMapper.toDocument(movie, List.of("movie")))
        .willReturn(movieDocument);

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(indexOperations).delete();
    verify(indexOperations).createWithMapping();
    verify(contentSearchRepository).saveAll(List.of(movieDocument));
  }

  @Test
  @DisplayName("DB 콘텐츠가 없어도 검색 인덱스가 없으면 인덱스를 생성한다.")
  void rebuildAllIfEmpty_success_create_index_when_content_is_empty() {
    // Given
    given(contentRepository.count()).willReturn(0L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(false);
    given(indexOperations.createWithMapping()).willReturn(true);

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(indexOperations).createWithMapping();
    verify(contentRepository, never()).findAll(any(PageRequest.class));
    verify(contentSearchRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("DB 콘텐츠가 없고 검색 인덱스에 문서가 남아 있으면 인덱스를 재생성한다.")
  void rebuildAllIfEmpty_success_recreate_index_when_content_is_empty_and_index_has_documents() {
    // Given
    given(contentRepository.count()).willReturn(0L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(true);
    given(contentSearchRepository.count()).willReturn(2L);
    given(indexOperations.delete()).willReturn(true);
    given(indexOperations.createWithMapping()).willReturn(true);

    // When
    contentSearchIndexService.rebuildAllIfEmpty();

    // Then
    verify(indexOperations).delete();
    verify(indexOperations).createWithMapping();
    verify(contentRepository, never()).findAll(any(PageRequest.class));
    verify(contentSearchRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("검색 인덱스 생성이 실패하면 재색인을 중단한다.")
  void rebuildAllIfEmpty_fail_when_create_index_fails() {
    // Given
    given(contentRepository.count()).willReturn(1L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(false);
    given(indexOperations.createWithMapping()).willReturn(false);

    // When & Then
    assertThatThrownBy(() -> contentSearchIndexService.rebuildAllIfEmpty())
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_SEARCH_INDEX_REBUILD_FAILED)
        );
    verify(contentRepository, never()).findAll(any(PageRequest.class));
    verify(contentSearchRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("검색 인덱스 삭제가 실패하면 재색인을 중단한다.")
  void rebuildAllIfEmpty_fail_when_delete_index_fails() {
    // Given
    given(contentRepository.count()).willReturn(1L);
    given(elasticsearchOperations.indexOps(ContentSearchDocument.class)).willReturn(indexOperations);
    given(indexOperations.exists()).willReturn(true);
    given(contentSearchRepository.count()).willReturn(2L);
    given(indexOperations.delete()).willReturn(false);

    // When & Then
    assertThatThrownBy(() -> contentSearchIndexService.rebuildAllIfEmpty())
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_SEARCH_INDEX_REBUILD_FAILED)
        );
    verify(indexOperations, never()).createWithMapping();
    verify(contentRepository, never()).findAll(any(PageRequest.class));
    verify(contentSearchRepository, never()).saveAll(anyList());
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

  @Test
  @DisplayName("콘텐츠 검색 인덱스 삭제 실패 시 예외를 전파하지 않는다.")
  void delete_success_when_repository_delete_fails() {
    // Given
    UUID contentId = UUID.randomUUID();
    willThrow(new RuntimeException("elasticsearch unavailable"))
        .given(contentSearchRepository)
        .deleteById(contentId.toString());

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

  private record TestContentTagNameProjection(
      UUID contentId,
      String tagName
  ) implements ContentTagNameProjection {

    @Override
    public UUID getContentId() {
      return contentId;
    }

    @Override
    public String getTagName() {
      return tagName;
    }
  }
}
