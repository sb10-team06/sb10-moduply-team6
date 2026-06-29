package com.team6.moduply.review.service;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.entity.Review;
import com.team6.moduply.review.exception.ReviewErrorCode;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.mapper.ReviewMapper;
import com.team6.moduply.review.repository.ReviewRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final ContentRepository contentRepository;

  @Transactional
  public ReviewDto create(ReviewCreateRequest request, ModuPlyUserDetails userDetails) {
    UUID authorId = userDetails.getUserDto().getId();
    UUID contentId = request.contentId();

    if (!contentRepository.existsById(contentId)) {
      throw new ReviewException(
          ReviewErrorCode.CONTENT_NOT_FOUND,
          Map.of("contentId", contentId)
      );
    }

    if (reviewRepository.existsByContentIdAndAuthorId(contentId, authorId)) {
      throw new ReviewException(
          ReviewErrorCode.REVIEW_ALREADY_EXISTS,
          Map.of("contentId", contentId, "authorId", authorId)
      );
    }

    Review review = Review.builder()
        .contentId(contentId)
        .authorId(authorId)
        .text(request.text())
        .rating(request.rating())
        .build();

    Review saved = reviewRepository.save(review);

    // TODO: S3 이미지 저장 로직 완성 후 profileImageUrl 실제 URL로 교체 필요 (현재 null 반환)
    ReviewDto.AuthorDto author = new ReviewDto.AuthorDto(
        authorId,
        userDetails.getUserDto().getName(),
        userDetails.getUserDto().getProfileImageUrl()
    );

    return reviewMapper.toDto(saved, author);
  }
}
