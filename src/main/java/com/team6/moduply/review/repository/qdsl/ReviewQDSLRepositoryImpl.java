package com.team6.moduply.review.repository.qdsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.dto.ReviewSortBy;
import com.team6.moduply.review.entity.QReview;
import com.team6.moduply.review.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewQDSLRepositoryImpl implements ReviewQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private final QReview review = QReview.review;

  @Override
  public List<Review> findAllWithCursor(ReviewSearchRequest request) {
    return queryFactory.selectFrom(review)
        .where(
            contentIdCondition(request.contentId()),
            cursorCondition(request)
        )
        .orderBy(orderSpecifiers(request))
        .limit((long) request.limit() + 1)
        .fetch();
  }

  @Override
  public long countWithCondition(ReviewSearchRequest request) {
    Long result = queryFactory.select(review.count())
        .from(review)
        .where(contentIdCondition(request.contentId()))
        .fetchOne();
    return result != null ? result : 0L;
  }

  @Override
  public ReviewStats calculateStatsByContentId(UUID contentId) {
    NumberExpression<Long> reviewCount = review.count();
    NumberExpression<Double> averageRating = review.rating.avg();

    Tuple result = queryFactory.select(reviewCount, averageRating)
        .from(review)
        .where(contentIdCondition(contentId))
        .fetchOne();

    if (result == null) {
      return new ReviewStats(0L, 0.0);
    }

    Long count = result.get(reviewCount);
    Double average = result.get(averageRating);
    return new ReviewStats(
        count != null ? count : 0L,
        average != null ? average : 0.0
    );
  }

  private BooleanExpression contentIdCondition(UUID contentId) {
    return contentId != null ? review.contentId.eq(contentId) : null;
  }

  private BooleanExpression cursorCondition(ReviewSearchRequest request) {
    if (request.cursor() == null) {
      return null;
    }

    if (request.sortBy() == ReviewSortBy.rating) {
      int colonIndex = request.cursor().indexOf(":");
      Double cursorRating = Double.parseDouble(request.cursor().substring(0, colonIndex));
      Instant cursorCreatedAt = Instant.parse(request.cursor().substring(colonIndex + 1));

      if (request.sortDirection() == SortDirection.ASCENDING) {
        return review.rating.gt(cursorRating)
            .or(review.rating.eq(cursorRating).and(review.createdAt.gt(cursorCreatedAt)))
            .or(review.rating.eq(cursorRating).and(review.createdAt.eq(cursorCreatedAt)).and(review.id.gt(request.idAfter())));
      } else {
        return review.rating.lt(cursorRating)
            .or(review.rating.eq(cursorRating).and(review.createdAt.lt(cursorCreatedAt)))
            .or(review.rating.eq(cursorRating).and(review.createdAt.eq(cursorCreatedAt)).and(review.id.gt(request.idAfter())));
      }
    }

    Instant cursorTime = Instant.parse(request.cursor());
    if (request.sortDirection() == SortDirection.ASCENDING) {
      return review.createdAt.gt(cursorTime)
          .or(review.createdAt.eq(cursorTime).and(review.id.gt(request.idAfter())));
    } else {
      return review.createdAt.lt(cursorTime)
          .or(review.createdAt.eq(cursorTime).and(review.id.gt(request.idAfter())));
    }
  }

  private OrderSpecifier<?>[] orderSpecifiers(ReviewSearchRequest request) {
    if (request.sortBy() == ReviewSortBy.rating) {
      return new OrderSpecifier<?>[] {
          primaryOrder(request),
          secondaryOrder(request),
          review.id.asc()
      };
    }
    return new OrderSpecifier<?>[] {
        primaryOrder(request),
        secondaryOrder(request)
    };
  }

  private OrderSpecifier<?> primaryOrder(ReviewSearchRequest request) {
    if (request.sortBy() == ReviewSortBy.rating) {
      return request.sortDirection() == SortDirection.ASCENDING
          ? review.rating.asc()
          : review.rating.desc();
    }
    return request.sortDirection() == SortDirection.ASCENDING
        ? review.createdAt.asc()
        : review.createdAt.desc();
  }

  private OrderSpecifier<?> secondaryOrder(ReviewSearchRequest request) {
    if (request.sortBy() == ReviewSortBy.rating) {
      return request.sortDirection() == SortDirection.ASCENDING
          ? review.createdAt.asc()
          : review.createdAt.desc();
    }
    return review.id.asc();
  }
}
