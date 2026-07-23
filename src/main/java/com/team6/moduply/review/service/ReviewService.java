package com.team6.moduply.review.service;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.config.CacheConfig;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.service.ContentService;
import com.team6.moduply.notification.event.FollowActivityEvent;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.dto.ReviewUpdateRequest;
import com.team6.moduply.review.entity.Review;
import com.team6.moduply.review.exception.ReviewErrorCode;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.mapper.ReviewMapper;
import com.team6.moduply.review.repository.ReviewRepository;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final ContentRepository contentRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final BinaryContentService binaryContentService;
  private final ContentService contentService;
  private final ReviewListCacheService reviewListCacheService;

  private void validateAuthor(Review review, UUID authorId, UUID reviewId) {
    if (!review.getAuthorId().equals(authorId)) {
      throw new ReviewException(
          ReviewErrorCode.REVIEW_FORBIDDEN,
          Map.of("reviewId", reviewId)
      );
    }
  }

  private ReviewDto.AuthorDto getAuthorDto(UUID authorId) {
    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("authorId", authorId)
        ));
    return new ReviewDto.AuthorDto(
        authorId,
        author.getName(),
        binaryContentService.generateUrl(author.getProfileImg())
    );
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.REVIEW_LIST, allEntries = true)
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

    ReviewDto.AuthorDto author = getAuthorDto(authorId);

    Review review = Review.builder()
        .contentId(contentId)
        .authorId(authorId)
        .text(request.text())
        .rating(request.rating())
        .build();

    Review saved = reviewRepository.save(review);
    contentService.refreshReviewStats(contentId);
    eventPublisher.publishEvent(new FollowActivityEvent(
        authorId,
        userDetails.getUserDto().getName(),
        "새 리뷰를 작성했습니다."
    ));

    return reviewMapper.toDto(saved, author);
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.REVIEW_LIST, allEntries = true)
  public ReviewDto update(UUID reviewId, ReviewUpdateRequest request,
      ModuPlyUserDetails userDetails) {
    UUID authorId = userDetails.getUserDto().getId();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("reviewId", reviewId)
        ));

    validateAuthor(review, authorId, reviewId);

    review.update(request.text(), request.rating());
    contentService.refreshReviewStats(review.getContentId());

    ReviewDto.AuthorDto author = getAuthorDto(authorId);
    return reviewMapper.toDto(review, author);
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.REVIEW_LIST, allEntries = true)
  public void delete(UUID reviewId, ModuPlyUserDetails userDetails) {
    UUID authorId = userDetails.getUserDto().getId();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("reviewId", reviewId)
        ));

    validateAuthor(review, authorId, reviewId);

    UUID contentId = review.getContentId();
    reviewRepository.delete(review);
    contentService.refreshReviewStats(contentId);
  }

  @Transactional(readOnly = true)
  public CursorResponse<ReviewDto> findAll(ReviewSearchRequest request) {
    return reviewListCacheService.findAll(request);
  }
}
