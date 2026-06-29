package com.team6.moduply.review.mapper;

import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
  public ReviewDto toDto(Review review, ReviewDto.AuthorDto author) {
    return new ReviewDto(
        review.getId(),
        review.getContentId(),
        author,
        review.getText(),
        review.getRating()
    );
  }
}
