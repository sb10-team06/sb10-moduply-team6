package com.team6.moduply.review;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.common.config.QueryDslConfig;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.dto.ReviewSortBy;
import com.team6.moduply.review.entity.Review;
import com.team6.moduply.review.repository.ReviewRepository;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(JpaAuditingConfig.class)
class ReviewRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private ReviewQDSLRepository reviewQDSLRepository;

  private Review saveReview(UUID contentId, UUID authorId, double rating) {
    return reviewRepository.save(Review.builder()
        .contentId(contentId)
        .authorId(authorId)
        .text("리뷰 내용")
        .rating(rating)
        .build());
  }

  @Test
  @DisplayName("콘텐츠 ID로 리뷰 목록을 조회한다.")
  void findAllWithCursor_success_with_content_id() {
    // given
    UUID contentId = UUID.randomUUID();
    saveReview(contentId, UUID.randomUUID(), 4.5);
    saveReview(contentId, UUID.randomUUID(), 3.0);
    saveReview(UUID.randomUUID(), UUID.randomUUID(), 5.0); // 다른 콘텐츠

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, null, null, 10, SortDirection.DESCENDING, ReviewSortBy.createdAt
    );

    // when
    List<Review> result = reviewQDSLRepository.findAllWithCursor(request);

    // then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(r -> r.getContentId().equals(contentId));
  }

  @Test
  @DisplayName("rating 기준 내림차순 정렬로 리뷰 목록을 조회한다.")
  void findAllWithCursor_success_with_rating_sort() {
    // given
    UUID contentId = UUID.randomUUID();
    saveReview(contentId, UUID.randomUUID(), 3.0);
    saveReview(contentId, UUID.randomUUID(), 5.0);
    saveReview(contentId, UUID.randomUUID(), 4.0);

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, null, null, 10, SortDirection.DESCENDING, ReviewSortBy.rating
    );

    // when
    List<Review> result = reviewQDSLRepository.findAllWithCursor(request);

    // then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getRating()).isEqualTo(5.0);
  }

  @Test
  @DisplayName("콘텐츠 ID로 리뷰 개수를 반환한다.")
  void countWithCondition_success() {
    // given
    UUID contentId = UUID.randomUUID();
    saveReview(contentId, UUID.randomUUID(), 4.5);
    saveReview(contentId, UUID.randomUUID(), 3.0);
    saveReview(UUID.randomUUID(), UUID.randomUUID(), 5.0);

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, null, null, 10, SortDirection.DESCENDING, ReviewSortBy.createdAt
    );

    // when
    long count = reviewQDSLRepository.countWithCondition(request);

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("limit보다 많은 리뷰가 있으면 limit+1개를 반환한다.")
  void findAllWithCursor_success_with_has_next() {
    // given
    UUID contentId = UUID.randomUUID();
    saveReview(contentId, UUID.randomUUID(), 4.5);
    saveReview(contentId, UUID.randomUUID(), 3.0);
    saveReview(contentId, UUID.randomUUID(), 5.0);

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, null, null, 2, SortDirection.DESCENDING, ReviewSortBy.createdAt
    );

    // when
    List<Review> result = reviewQDSLRepository.findAllWithCursor(request);

    // then
    assertThat(result).hasSize(3); // limit+1
  }

  @Test
  @DisplayName("cursor와 idAfter로 다음 페이지 리뷰 목록을 조회한다.")
  void findAllWithCursor_success_with_cursor() throws InterruptedException {
    // given
    UUID contentId = UUID.randomUUID();
    Review review1 = saveReview(contentId, UUID.randomUUID(), 4.5);
    Thread.sleep(10); // createdAt 차이를 위해
    Review review2 = saveReview(contentId, UUID.randomUUID(), 3.0);

    String cursor = review2.getCreatedAt().toString();
    UUID idAfter = review2.getId();

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, cursor, idAfter, 10, SortDirection.DESCENDING, ReviewSortBy.createdAt
    );

    // when
    List<Review> result = reviewQDSLRepository.findAllWithCursor(request);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(review1.getId());
  }
}
