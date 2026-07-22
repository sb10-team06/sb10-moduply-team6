package com.team6.moduply.content.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.elastic.clients.elasticsearch._types.SortOrder;
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
import org.mockito.ArgumentCaptor;
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
  @DisplayName("검색 결과가 limit보다 많으면 다음 페이지 커서를 반환한다.")
  void search_success_with_next_cursor() {
    // Given
    UUID firstContentId = UUID.randomUUID();
    UUID secondContentId = UUID.randomUUID();
    ContentSearchDocument firstDocument = ContentSearchDocument.builder()
        .id(firstContentId.toString())
        .build();
    ContentSearchDocument secondDocument = ContentSearchDocument.builder()
        .id(secondContentId.toString())
        .build();
    SearchHit<ContentSearchDocument> firstHit = new SearchHit<>(
        "contents",
        firstContentId.toString(),
        null,
        12.5f,
        new Object[]{12.5f, "2026-07-21T00:00:00Z", firstContentId.toString()},
        null,
        null,
        null,
        null,
        null,
        firstDocument
    );
    SearchHit<ContentSearchDocument> secondHit = new SearchHit<>(
        "contents",
        secondContentId.toString(),
        null,
        10.0f,
        new Object[]{10.0f, "2026-07-20T00:00:00Z", secondContentId.toString()},
        null,
        null,
        null,
        null,
        null,
        secondDocument
    );
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of(firstHit, secondHit));
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(2L);

    // When
    ContentSearchResult result = contentSearchService.search(
        ContentType.movie,
        "악마",
        List.of("스릴러"),
        null,
        null,
        1,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result.contentIds()).containsExactly(firstContentId);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo("12.5|2026-07-21T00:00:00Z");
    assertThat(result.nextIdAfter()).isEqualTo(firstContentId);
    assertThat(result.totalCount()).isEqualTo(2L);
  }

  @Test
  @DisplayName("다음 페이지 조회 시 score, 정렬값, id를 search_after로 전달한다.")
  void search_success_with_search_after() {
    // Given
    UUID idAfter = UUID.randomUUID();
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of());
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(0L);

    // When
    contentSearchService.search(
        ContentType.movie,
        "악마",
        List.of(),
        "12.5|2026-07-21T00:00:00Z",
        idAfter,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(elasticsearchOperations).search(queryCaptor.capture(), eq(ContentSearchDocument.class));
    assertThat(queryCaptor.getValue().getSearchAfter())
        .containsExactly(12.5f, "2026-07-21T00:00:00Z", idAfter.toString());
  }

  @Test
  @DisplayName("날짜 커서가 epoch millis 형식이면 숫자 정렬값으로 search_after를 전달한다.")
  void search_success_with_epoch_millis_date_cursor() {
    // Given
    UUID idAfter = UUID.randomUUID();
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of());
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(0L);

    // When
    contentSearchService.search(
        ContentType.movie,
        "악마",
        List.of(),
        "12.5|1784592000000",
        idAfter,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(elasticsearchOperations).search(queryCaptor.capture(), eq(ContentSearchDocument.class));
    assertThat(queryCaptor.getValue().getSearchAfter())
        .containsExactly(12.5f, 1784592000000L, idAfter.toString());
  }

  @Test
  @DisplayName("검색어가 있으면 관련도 점수를 우선 정렬하고 요청 정렬값을 보조 정렬로 사용한다.")
  void search_success_with_score_first_sort() {
    // Given
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of());
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(0L);

    // When
    contentSearchService.search(
        ContentType.movie,
        "악마",
        List.of(),
        null,
        null,
        20,
        ContentSortBy.rate,
        SortDirection.ASCENDING
    );

    // Then
    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(elasticsearchOperations).search(queryCaptor.capture(), eq(ContentSearchDocument.class));
    NativeQuery query = queryCaptor.getValue();
    assertThat(query.getSortOptions()).hasSize(3);
    assertThat(query.getSortOptions().get(0).isScore()).isTrue();
    assertThat(query.getSortOptions().get(0).score().order()).isEqualTo(SortOrder.Desc);
    assertThat(query.getSortOptions().get(1).field().field()).isEqualTo("averageRating");
    assertThat(query.getSortOptions().get(1).field().order()).isEqualTo(SortOrder.Asc);
    assertThat(query.getSortOptions().get(2).field().field()).isEqualTo("id.keyword");
  }

  @Test
  @DisplayName("인기순 검색은 리뷰 수, 평균 평점, 생성일을 보조 정렬로 사용한다.")
  void search_success_with_popularity_sort() {
    // Given
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of());
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(0L);

    // When
    contentSearchService.search(
        null,
        "악마",
        List.of(),
        null,
        null,
        20,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );

    // Then
    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(elasticsearchOperations).search(queryCaptor.capture(), eq(ContentSearchDocument.class));
    NativeQuery query = queryCaptor.getValue();
    assertThat(query.getSortOptions()).hasSize(5);
    assertThat(query.getSortOptions().get(0).isScore()).isTrue();
    assertThat(query.getSortOptions().get(1).field().field()).isEqualTo("reviewCount");
    assertThat(query.getSortOptions().get(1).field().order()).isEqualTo(SortOrder.Desc);
    assertThat(query.getSortOptions().get(2).field().field()).isEqualTo("averageRating");
    assertThat(query.getSortOptions().get(2).field().order()).isEqualTo(SortOrder.Desc);
    assertThat(query.getSortOptions().get(3).field().field()).isEqualTo("createdAt");
    assertThat(query.getSortOptions().get(3).field().order()).isEqualTo(SortOrder.Desc);
    assertThat(query.getSortOptions().get(4).field().field()).isEqualTo("id.keyword");
  }

  @Test
  @DisplayName("인기순 다음 페이지 조회 시 리뷰 수, 평균 평점, 생성일, id를 search_after로 전달한다.")
  void search_success_with_popularity_search_after() {
    // Given
    UUID idAfter = UUID.randomUUID();
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of());
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(0L);

    // When
    contentSearchService.search(
        null,
        "악마",
        List.of(),
        "12.5|10|4.5|2026-07-21T00:00:00Z",
        idAfter,
        20,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );

    // Then
    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(elasticsearchOperations).search(queryCaptor.capture(), eq(ContentSearchDocument.class));
    assertThat(queryCaptor.getValue().getSearchAfter())
        .containsExactly(12.5f, 10L, 4.5, "2026-07-21T00:00:00Z", idAfter.toString());
  }

  @Test
  @DisplayName("인기순 검색 결과가 limit보다 많으면 복합 정렬값으로 다음 페이지 커서를 반환한다.")
  void search_success_with_popularity_next_cursor() {
    // Given
    UUID firstContentId = UUID.randomUUID();
    UUID secondContentId = UUID.randomUUID();
    ContentSearchDocument firstDocument = ContentSearchDocument.builder()
        .id(firstContentId.toString())
        .build();
    ContentSearchDocument secondDocument = ContentSearchDocument.builder()
        .id(secondContentId.toString())
        .build();
    SearchHit<ContentSearchDocument> firstHit = new SearchHit<>(
        "contents",
        firstContentId.toString(),
        null,
        12.5f,
        new Object[]{12.5f, 10, 4.5, "2026-07-21T00:00:00Z", firstContentId.toString()},
        null,
        null,
        null,
        null,
        null,
        firstDocument
    );
    SearchHit<ContentSearchDocument> secondHit = new SearchHit<>(
        "contents",
        secondContentId.toString(),
        null,
        10.0f,
        new Object[]{10.0f, 8, 4.0, "2026-07-20T00:00:00Z", secondContentId.toString()},
        null,
        null,
        null,
        null,
        null,
        secondDocument
    );
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of(firstHit, secondHit));
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(2L);

    // When
    ContentSearchResult result = contentSearchService.search(
        null,
        "악마",
        List.of(),
        null,
        null,
        1,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result.contentIds()).containsExactly(firstContentId);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo("12.5|10|4.5|2026-07-21T00:00:00Z");
    assertThat(result.nextIdAfter()).isEqualTo(firstContentId);
  }

  @Test
  @DisplayName("검색어가 여러 단어이면 기본 검색 조건에 최소 매칭 비율을 적용한다.")
  void search_success_with_keyword_minimum_should_match() {
    // Given
    SearchHits<ContentSearchDocument> searchHits = org.mockito.Mockito.mock(SearchHits.class);
    given(searchHits.getSearchHits()).willReturn(List.of());
    given(elasticsearchOperations.search(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(searchHits);
    given(elasticsearchOperations.count(any(NativeQuery.class), eq(ContentSearchDocument.class)))
        .willReturn(0L);

    // When
    contentSearchService.search(
        null,
        "눈 깜짝할 사이에",
        List.of(),
        null,
        null,
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(elasticsearchOperations).search(queryCaptor.capture(), eq(ContentSearchDocument.class));
    NativeQuery query = queryCaptor.getValue();
    assertThat(query.getQuery()
        .bool()
        .must()
        .get(0)
        .bool()
        .should()
        .get(0)
        .multiMatch()
        .minimumShouldMatch()
    ).isEqualTo("2<75%");
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
