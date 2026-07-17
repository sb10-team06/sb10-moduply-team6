package com.team6.moduply.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository.ReviewStats;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepositoryImpl;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewQDSLRepositoryImplTest {

  @Mock
  private JPAQueryFactory queryFactory;

  @Mock
  private JPAQuery<Tuple> query;

  @Mock
  private Tuple tuple;

  @Test
  @DisplayName("리뷰 통계 집계 쿼리 결과가 null이면 기본 통계를 반환한다.")
  void calculateStatsByContentId_success_when_query_result_is_null() {
    // Given
    ReviewQDSLRepositoryImpl repository = new ReviewQDSLRepositoryImpl(queryFactory);
    given(queryFactory.select(any(Expression[].class))).willReturn(query);
    given(query.from(any(EntityPath.class))).willReturn(query);
    given(query.where(any(Predicate.class))).willReturn(query);
    given(query.fetchOne()).willReturn(null);

    // When
    ReviewStats stats = repository.calculateStatsByContentId(UUID.randomUUID());

    // Then
    assertThat(stats.reviewCount()).isZero();
    assertThat(stats.averageRating()).isZero();
  }

  @Test
  @DisplayName("리뷰 통계 집계 결과의 리뷰 수가 null이면 0으로 반환한다.")
  void calculateStatsByContentId_success_when_count_is_null() {
    // Given
    ReviewQDSLRepositoryImpl repository = new ReviewQDSLRepositoryImpl(queryFactory);
    given(queryFactory.select(any(Expression[].class))).willReturn(query);
    given(query.from(any(EntityPath.class))).willReturn(query);
    given(query.where(any(Predicate.class))).willReturn(query);
    given(query.fetchOne()).willReturn(tuple);
    doReturn(null, 4.0).when(tuple).get(any(Expression.class));

    // When
    ReviewStats stats = repository.calculateStatsByContentId(UUID.randomUUID());

    // Then
    assertThat(stats.reviewCount()).isZero();
    assertThat(stats.averageRating()).isEqualTo(4.0);
  }
}
