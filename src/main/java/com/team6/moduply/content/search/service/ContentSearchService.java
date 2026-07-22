package com.team6.moduply.content.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
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
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
public class ContentSearchService {

  private static final String CURSOR_DELIMITER = "|";
  private static final List<String> SEARCH_FIELDS = List.of("title^5", "tags^3", "description");
  private static final String KEYWORD_MINIMUM_SHOULD_MATCH = "2<75%";

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
    ContentSortBy searchSortBy = sortBy;
    NativeQueryBuilder queryBuilder = NativeQuery.builder()
        .withQuery(buildQuery(
            typeEqual,
            keywordLike,
            tagsIn
        ))
        .withMaxResults(limit + 1)
        .withTrackTotalHits(true);
    applySort(queryBuilder, searchSortBy, sortDirection);

    List<Object> searchAfter = buildSearchAfter(cursor, idAfter, searchSortBy);
    if (!searchAfter.isEmpty()) {
      queryBuilder.withSearchAfter(searchAfter);
    }

    NativeQuery query = queryBuilder.build();
    NativeQuery countQuery = NativeQuery.builder()
        .withQuery(buildQuery(
            typeEqual,
            keywordLike,
            tagsIn
        ))
        .build();

    SearchHits<ContentSearchDocument> searchHits = elasticsearchOperations.search(
        query,
        ContentSearchDocument.class
    );
    long totalCount = elasticsearchOperations.count(countQuery, ContentSearchDocument.class);

    List<SearchHit<ContentSearchDocument>> hits = searchHits.getSearchHits();
    boolean hasNext = hits.size() > limit;
    List<SearchHit<ContentSearchDocument>> pageHits = hasNext
        ? hits.subList(0, limit)
        : hits;
    List<UUID> contentIds = pageHits.stream()
        .map(hit -> UUID.fromString(hit.getContent().getId()))
        .toList();
    SearchHit<ContentSearchDocument> lastHit = pageHits.isEmpty()
        ? null
        : pageHits.get(pageHits.size() - 1);

    return new ContentSearchResult(
        contentIds,
        hasNext,
        hasNext ? extractCursor(lastHit, searchSortBy) : null,
        hasNext ? UUID.fromString(lastHit.getContent().getId()) : null,
        totalCount
    );
  }

  private Query buildQuery(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn
  ) {
    return Query.of(query -> query.bool(bool -> {
      bool.must(buildKeywordQuery(keywordLike));

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

      return bool;
    }));
  }

  private Query buildKeywordQuery(String keywordLike) {
    return Query.of(query -> query.bool(bool -> bool
        .should(should -> should.multiMatch(multiMatch -> multiMatch
            .query(keywordLike)
            .fields(SEARCH_FIELDS)
            .minimumShouldMatch(KEYWORD_MINIMUM_SHOULD_MATCH)
        ))
        .should(should -> should.multiMatch(multiMatch -> multiMatch
            .query(keywordLike)
            .fields(SEARCH_FIELDS)
            .type(TextQueryType.Phrase)
        ))
        .should(should -> should.multiMatch(multiMatch -> multiMatch
            .query(keywordLike)
            .fields(SEARCH_FIELDS)
            .type(TextQueryType.PhrasePrefix)
        ))
        .minimumShouldMatch("1")
    ));
  }

  private String toSearchSortField(ContentSortBy sortBy) {
    if (sortBy == ContentSortBy.rate) {
      return "averageRating";
    }

    return "createdAt";
  }

  private void applySort(
      NativeQueryBuilder queryBuilder,
      ContentSortBy sortBy,
      SortDirection sortDirection
  ) {
    SortOrder sortOrder = toSearchSortOrder(sortDirection);
    queryBuilder.withSort(sort -> sort.score(score -> score.order(SortOrder.Desc)));

    if (sortBy == ContentSortBy.watcherCount) {
      queryBuilder.withSort(sort -> sort.field(field -> field
          .field("reviewCount")
          .order(sortOrder)
      ));
      queryBuilder.withSort(sort -> sort.field(field -> field
          .field("averageRating")
          .order(sortOrder)
      ));
      queryBuilder.withSort(sort -> sort.field(field -> field
          .field("createdAt")
          .order(sortOrder)
      ));
    } else {
      queryBuilder.withSort(sort -> sort.field(field -> field
          .field(toSearchSortField(sortBy))
          .order(sortOrder)
      ));
    }

    queryBuilder.withSort(sort -> sort.field(field -> field
        .field("id.keyword")
        .order(sortOrder)
    ));
  }

  private SortOrder toSearchSortOrder(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING ? SortOrder.Asc : SortOrder.Desc;
  }

  private List<Object> buildSearchAfter(String cursor, UUID idAfter, ContentSortBy sortBy) {
    if (cursor == null || idAfter == null) {
      return List.of();
    }

    String[] cursorValues = cursor.split("\\|");
    if (sortBy == ContentSortBy.watcherCount) {
      return buildWatcherCountSearchAfter(cursorValues, idAfter, cursor);
    }

    if (cursorValues.length != 2) {
      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          Map.of("cursor", cursor)
      );
    }

    return List.of(
        parseScoreCursor(cursorValues[0]),
        parseSortCursor(cursorValues[1], sortBy),
        idAfter.toString()
    );
  }

  private List<Object> buildWatcherCountSearchAfter(
      String[] cursorValues,
      UUID idAfter,
      String cursor
  ) {
    if (cursorValues.length != 4) {
      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          Map.of("cursor", cursor)
      );
    }

    return List.of(
        parseScoreCursor(cursorValues[0]),
        parseLongCursor(ContentSortBy.watcherCount, cursorValues[1]),
        parseDecimalCursor(cursorValues[2]).doubleValue(),
        parseDateSortCursor(cursorValues[3]),
        idAfter.toString()
    );
  }

  private String extractCursor(SearchHit<ContentSearchDocument> hit, ContentSortBy sortBy) {
    List<Object> sortValues = hit.getSortValues();
    if (sortValues.size() < 2) {
      return null;
    }

    if (sortBy == ContentSortBy.watcherCount) {
      if (sortValues.size() < 4) {
        return null;
      }
      return "%s%s%s%s%s%s%s".formatted(
          sortValues.get(0),
          CURSOR_DELIMITER,
          sortValues.get(1),
          CURSOR_DELIMITER,
          sortValues.get(2),
          CURSOR_DELIMITER,
          sortValues.get(3)
      );
    }

    return "%s%s%s".formatted(
        sortValues.get(0),
        CURSOR_DELIMITER,
        sortValues.get(1)
    );
  }

  private float parseScoreCursor(String cursor) {
    try {
      return Float.parseFloat(cursor);
    } catch (NumberFormatException e) {
      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          Map.of("sortBy", "_score", "cursor", cursor)
      );
    }
  }

  private Object parseSortCursor(String cursor, ContentSortBy sortBy) {
    if (sortBy == ContentSortBy.createdAt) {
      return parseDateSortCursor(cursor);
    }

    return parseDecimalCursor(cursor).doubleValue();
  }

  private Object parseDateSortCursor(String cursor) {
    try {
      return Long.parseLong(cursor);
    } catch (NumberFormatException ignored) {
      return parseInstantCursor(cursor).toString();
    }
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

  private long parseLongCursor(ContentSortBy sortBy, String cursor) {
    try {
      return Long.parseLong(cursor);
    } catch (NumberFormatException e) {
      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          Map.of("sortBy", sortBy, "cursor", cursor)
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
      boolean hasNext,
      String nextCursor,
      UUID nextIdAfter,
      long totalCount
  ) {
  }
}
