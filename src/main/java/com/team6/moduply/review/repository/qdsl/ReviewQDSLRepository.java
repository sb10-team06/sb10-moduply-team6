package com.team6.moduply.review.repository.qdsl;

import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.entity.Review;
import java.util.List;
import java.util.UUID;

public interface ReviewQDSLRepository {

  List<Review> findAllWithCursor(ReviewSearchRequest request);

  long countWithCondition(ReviewSearchRequest request);

  ReviewStats calculateStatsByContentId(UUID contentId);

  record ReviewStats(
      long reviewCount,
      double averageRating
  ) {
  }
}
