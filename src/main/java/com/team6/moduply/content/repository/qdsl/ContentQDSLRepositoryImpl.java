package com.team6.moduply.content.repository.qdsl;

import static com.team6.moduply.content.entity.QContent.content;
import static com.team6.moduply.content.entity.QContentTag.contentTag;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContentQDSLRepositoryImpl implements ContentQDSLRepository {

  @PersistenceContext
  private EntityManager entityManager;

  // 검색 조건, 커서 조건, 정렬 조건을 조합하여 콘텐츠 목록을 조회한다.
  @Override
  public List<Content> findContents(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn,
      String cursor,
      UUID idAfter,
      int limit,
      ContentSortBy sortBy,
      SortDirection sortDirection
  ) {
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    List<BooleanExpression> conditions = buildSearchConditions(typeEqual, keywordLike, tagsIn);

    conditions.add(buildCursorCondition(sortBy, sortDirection, cursor, idAfter));

    return queryFactory
        .selectFrom(content)
        .where(toWhereArray(conditions))
        .orderBy(buildOrderSpecifiers(sortBy, sortDirection))
        .limit(limit)
        .fetch();
  }

  // 현재 검색 조건에 해당하는 콘텐츠 전체 개수를 조회한다.
  @Override
  public long countContents(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn
  ) {
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

    Long count = queryFactory
        .select(content.id.countDistinct())
        .from(content)
        .where(toWhereArray(buildSearchConditions(typeEqual, keywordLike, tagsIn)))
        .fetchOne();

    return count == null ? 0 : count;
  }

  // 타입, 검색어, 태그 필터를 QueryDSL 조건식으로 변환한다.
  private List<BooleanExpression> buildSearchConditions(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn
  ) {
    List<BooleanExpression> conditions = new ArrayList<>();

    if (typeEqual != null) {
      conditions.add(content.type.eq(typeEqual));
    }

    if (keywordLike != null && !keywordLike.isBlank()) {
      conditions.add(
          content.title.containsIgnoreCase(keywordLike)
              .or(content.description.containsIgnoreCase(keywordLike))
      );
    }

    if (tagsIn != null && !tagsIn.isEmpty()) {
      conditions.add(
          JPAExpressions
              .selectOne()
              .from(contentTag)
              .where(
                  contentTag.content.eq(content),
                  contentTag.tag.tagName.in(tagsIn)
              )
              .exists()
      );
    }

    return conditions;
  }

  // 정렬 기준에 맞는 커서 조건을 선택하여 다음 페이지 조회 범위를 만든다.
  private BooleanExpression buildCursorCondition(
      ContentSortBy sortBy,
      SortDirection sortDirection,
      String cursorValue,
      UUID idAfter
  ) {
    if (cursorValue == null && idAfter == null) {
      return null;
    }

    if (cursorValue == null || idAfter == null) {
      Map<String, Object> details = new HashMap<>();
      details.put("cursor", cursorValue);
      details.put("idAfter", idAfter);

      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          details
      );
    }

    Object parsedCursor = parseCursorValue(sortBy, cursorValue);

    return switch (sortBy) {
      case createdAt -> buildCreatedAtCursorCondition((Instant) parsedCursor, idAfter, sortDirection);
      case watcherCount -> buildWatcherCountCursorCondition((Long) parsedCursor, idAfter, sortDirection);
      case rate -> buildAverageRatingCursorCondition((BigDecimal) parsedCursor, idAfter, sortDirection);
    };
  }

  // 선택한 정렬 기준과 ID 보조 정렬을 함께 적용하여 안정적인 페이지 순서를 만든다.
  private OrderSpecifier<?>[] buildOrderSpecifiers(
      ContentSortBy sortBy,
      SortDirection sortDirection
  ) {
    OrderSpecifier<?> sortOrder = switch (sortBy) {
      case createdAt -> sortDirection == SortDirection.ASCENDING
          ? content.createdAt.asc()
          : content.createdAt.desc();
      case watcherCount -> sortDirection == SortDirection.ASCENDING
          ? content.watcherCount.asc()
          : content.watcherCount.desc();
      case rate -> sortDirection == SortDirection.ASCENDING
          ? content.averageRating.asc()
          : content.averageRating.desc();
    };
    OrderSpecifier<UUID> idOrder = sortDirection == SortDirection.ASCENDING
        ? content.id.asc()
        : content.id.desc();

    return new OrderSpecifier<?>[]{sortOrder, idOrder};
  }

  // 생성일 기준 커서 조건을 만든다.
  private BooleanExpression buildCreatedAtCursorCondition(
      Instant cursorValue,
      UUID idAfter,
      SortDirection sortDirection
  ) {
    if (sortDirection == SortDirection.ASCENDING) {
      return content.createdAt.gt(cursorValue)
          .or(content.createdAt.eq(cursorValue).and(content.id.gt(idAfter)));
    }

    return content.createdAt.lt(cursorValue)
        .or(content.createdAt.eq(cursorValue).and(content.id.lt(idAfter)));
  }

  // 시청자 수 기준 커서 조건을 만든다.
  private BooleanExpression buildWatcherCountCursorCondition(
      Long cursorValue,
      UUID idAfter,
      SortDirection sortDirection
  ) {
    if (sortDirection == SortDirection.ASCENDING) {
      return content.watcherCount.gt(cursorValue)
          .or(content.watcherCount.eq(cursorValue).and(content.id.gt(idAfter)));
    }

    return content.watcherCount.lt(cursorValue)
        .or(content.watcherCount.eq(cursorValue).and(content.id.lt(idAfter)));
  }

  // 평균 평점 기준 커서 조건을 만든다.
  private BooleanExpression buildAverageRatingCursorCondition(
      BigDecimal cursorValue,
      UUID idAfter,
      SortDirection sortDirection
  ) {
    if (sortDirection == SortDirection.ASCENDING) {
      return content.averageRating.gt(cursorValue)
          .or(content.averageRating.eq(cursorValue).and(content.id.gt(idAfter)));
    }

    return content.averageRating.lt(cursorValue)
        .or(content.averageRating.eq(cursorValue).and(content.id.lt(idAfter)));
  }

  // null 조건을 제외하고 QueryDSL where 절에 전달할 배열로 변환한다.
  private BooleanExpression[] toWhereArray(List<BooleanExpression> conditions) {
    return conditions.stream()
        .filter(condition -> condition != null)
        .toArray(BooleanExpression[]::new);
  }

  // 문자열 커서를 정렬 기준에 맞는 Java 타입으로 변환한다.
  private Object parseCursorValue(ContentSortBy sortBy, String cursor) {
    try {
      return switch (sortBy) {
        case createdAt -> Instant.parse(cursor);
        case watcherCount -> Long.parseLong(cursor);
        case rate -> new BigDecimal(cursor);
      };
    } catch (DateTimeParseException | NumberFormatException e) {
      throw new ContentException(
          ContentErrorCode.INVALID_CURSOR,
          Map.of("sortBy", sortBy, "cursor", cursor)
      );
    }
  }
}
