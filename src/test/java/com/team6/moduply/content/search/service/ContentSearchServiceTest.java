package com.team6.moduply.content.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.search.document.ContentSearchDocument;
import com.team6.moduply.content.search.service.ContentSearchService.ContentSearchResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

@ExtendWith(MockitoExtension.class)
class ContentSearchServiceTest {

  @Mock
  private ElasticsearchOperations elasticsearchOperations;

  @InjectMocks
  private ContentSearchService contentSearchService;

  @Test
  @DisplayName("Elasticsearch 검색 결과를 콘텐츠 ID 목록과 전체 개수로 반환한다.")
  void search_success() {
    // Given
    UUID contentId = UUID.randomUUID();
    ContentSearchDocument document = ContentSearchDocument.builder()
        .id(contentId.toString())
        .build();
    SearchHit<ContentSearchDocument> searchHit = new SearchHit<>(
        "contents",
        contentId.toString(),
        null,
        1.0f,
        null,
        null,
        null,
        null,
        null,
        null,
        document
    );
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of(searchHit));
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(1L);

    // When
    ContentSearchResult result = contentSearchService.search(
        ContentType.movie,
        "inception",
        List.of("SF"),
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result.contentIds()).containsExactly(contentId);
    assertThat(result.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("정렬 기준에 맞지 않는 커서 값이면 예외를 던진다.")
  void search_fail_with_invalid_cursor() {
    // When & Then
    assertThatThrownBy(() -> contentSearchService.search(
        null,
        "inception",
        List.of(),
        "invalid-cursor",
        UUID.randomUUID(),
        20,
        ContentSortBy.rate,
        SortDirection.DESCENDING
    )).isInstanceOf(ContentException.class);
    verify(elasticsearchOperations, never()).search(any(NativeQuery.class), eq(ContentSearchDocument.class));
  }
}
