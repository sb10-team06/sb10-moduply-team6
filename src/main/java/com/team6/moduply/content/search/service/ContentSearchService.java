package com.team6.moduply.content.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.search.document.ContentSearchDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContentSearchService {

  private static final List<String> SEARCH_FIELDS = List.of("title^3", "description", "tags");

  private final ElasticsearchOperations elasticsearchOperations;

  public ContentSearchService(ElasticsearchOperations elasticsearchOperations) {
    this.elasticsearchOperations = elasticsearchOperations;
  }

  public ContentSearchResult search(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn,
      String cursor,
      UUID idAfter,
      int limit,
      ContentSortBy sortBy,
      SortDirection sortDirection
  ) {
    ContentSortBy searchSortBy = toSearchSortBy(sortBy);
    NativeQuery query = NativeQuery.builder()
        .withQuery(buildQuery(
            typeEqual,
            keywordLike,
            tagsIn,
            cursor,
            idAfter,
            searchSortBy,
            sortDirection
        ))
        .withSort(sort -> sort.field(field -> field
            .field(toSearchSortField(searchSortBy))
            .order(toSearchSortOrder(sortDirection))
        ))
        .withSort(sort -> sort.field(field -> field
            .field("id.keyword")
            .order(toSearchSortOrder(sortDirection))
        ))
        .withMaxResults(limit)
        .withTrackTotalHits(true)
        .build();
    NativeQuery countQuery = NativeQuery.builder()
        .withQuery(buildQuery(
            typeEqual,
            keywordLike,
            tagsIn,
            null,
            null,
            searchSortBy,
            sortDirection
        ))
        .build();

    SearchHits<ContentSearchDocument> searchHits = elasticsearchOperations.search(
        query,
        ContentSearchDocument.class
    );
    long totalCount = elasticsearchOperations.count(countQuery, ContentSearchDocument.class);

    List<UUID> contentIds = searchHits.getSearchHits().stream()
        .map(hit -> UUID.fromString(hit.getContent().getId()))
        .toList();

    return new ContentSearchResult(contentIds, totalCount);
  }

  private Query buildQuery(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn,
      String cursor,
      UUID idAfter,
      ContentSortBy sortBy,
      SortDirection sortDirection
  ) {
    return Query.of(query -> query.bool(bool -> {
      bool.must(must -> must.multiMatch(multiMatch -> multiMatch
          .query(keywordLike)
          .fields(SEARCH_FIELDS)
      ));

      if (typeEqual != null) {
        bool.filter(filter -> filter.term(term -> term
            .field("type.keyword")
            .value(typeEqual.name())
        ));
      }

      if (tagsIn != null && !tagsIn.isEmpty()) {
        bool.filter(filter -> filter.terms(terms -> terms
            .field("tags.keyword")
            .terms(values -> values.value(tagsIn.stream()
                .map(FieldValue::of)
                .toList()))
        ));
      }

      if (StringUtils.hasText(cursor) && idAfter != null) {
        bool.filter(buildCursorQuery(sortBy, sortDirection, cursor, idAfter));
      }

      return bool;
    }));
  }

  private Query buildCursorQuery(
      ContentSortBy sortBy,
      SortDirection sortDirection,
      String cursor,
      UUID idAfter
  ) {
    String sortField = toSearchSortField(sortBy);

    return Query.of(query -> query.bool(bool -> bool
        .should(buildCursorRangeQuery(sortBy, sortDirection, sortField, cursor))
        .should(equalSortValueAndAfterId(sortBy, sortDirection, sortField, cursor, idAfter))
        .minimumShouldMatch("1")
    ));
  }

  private Query equalSortValueAndAfterId(
      ContentSortBy sortBy,
      SortDirection sortDirection,
      String sortField,
      String cursor,
      UUID idAfter
  ) {
    return Query.of(query -> query.bool(bool -> bool
        .filter(buildSortValueEqualsQuery(sortBy, sortField, cursor))
        .filter(buildIdRangeQuery(sortDirection, idAfter))
    ));
  }

  private Query buildCursorRangeQuery(
      ContentSortBy sortBy,
      SortDirection sortDirection,
      String sortField,
      String cursor
  ) {
    if (sortBy == ContentSortBy.createdAt) {
      return Query.of(query -> query.range(range -> range.date(date -> {
        date.field(sortField);
        if (sortDirection == SortDirection.ASCENDING) {
          return date.gt(cursor);
        }
        return date.lt(cursor);
      })));
    }

    BigDecimal cursorValue = parseDecimalCursor(cursor);
    return Query.of(query -> query.range(range -> range.number(number -> {
      number.field(sortField);
      if (sortDirection == SortDirection.ASCENDING) {
        return number.gt(cursorValue.doubleValue());
      }
      return number.lt(cursorValue.doubleValue());
    })));
  }

  private Query buildSortValueEqualsQuery(
      ContentSortBy sortBy,
      String sortField,
      String cursor
  ) {
    if (sortBy == ContentSortBy.createdAt) {
      parseInstantCursor(cursor);
      return Query.of(query -> query.term(term -> term
          .field(sortField)
          .value(cursor)
      ));
    }

    return Query.of(query -> query.term(term -> term
        .field(sortField)
        .value(parseDecimalCursor(cursor).doubleValue())
    ));
  }

  private Query buildIdRangeQuery(SortDirection sortDirection, UUID idAfter) {
    return Query.of(query -> query.range(range -> range.term(term -> {
      term.field("id.keyword");
      if (sortDirection == SortDirection.ASCENDING) {
        return term.gt(idAfter.toString());
      }
      return term.lt(idAfter.toString());
    })));
  }

  private String toSearchSortField(ContentSortBy sortBy) {
    if (sortBy == ContentSortBy.rate) {
      return "averageRating";
    }

    return "createdAt";
  }

  private ContentSortBy toSearchSortBy(ContentSortBy sortBy) {
    // 검색 인덱스에는 실시간 시청자 수를 저장하지 않으므로 watcherCount 정렬은 최신순으로 대체한다.
    if (sortBy == ContentSortBy.watcherCount) {
      return ContentSortBy.createdAt;
    }

    return sortBy;
  }

  private SortOrder toSearchSortOrder(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING ? SortOrder.Asc : SortOrder.Desc;
  }

  private Instant parseInstantCursor(String cursor) {
    try {
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          Map.of("sortBy", ContentSortBy.createdAt, "cursor", cursor)
      );
    }
  }

  private BigDecimal parseDecimalCursor(String cursor) {
    try {
      return new BigDecimal(cursor);
    } catch (NumberFormatException e) {
      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          Map.of("sortBy", ContentSortBy.rate, "cursor", cursor)
      );
    }
  }

  public record ContentSearchResult(
      List<UUID> contentIds,
      long totalCount
  ) {
  }
}
