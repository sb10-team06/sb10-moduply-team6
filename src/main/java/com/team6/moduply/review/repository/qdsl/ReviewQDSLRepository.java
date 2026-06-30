package com.team6.moduply.review.repository.qdsl;

import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.entity.Review;
import java.util.List;

public interface ReviewQDSLRepository {

  List<Review> findAllWithCursor(ReviewSearchRequest request);

  long countWithCondition(ReviewSearchRequest request);
}
