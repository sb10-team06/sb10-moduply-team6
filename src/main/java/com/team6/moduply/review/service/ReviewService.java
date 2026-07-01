package com.team6.moduply.review.service;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.dto.ReviewSortBy;
import com.team6.moduply.review.dto.ReviewUpdateRequest;
import com.team6.moduply.review.entity.Review;
import com.team6.moduply.review.exception.ReviewErrorCode;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.mapper.ReviewMapper;
import com.team6.moduply.review.repository.ReviewRepository;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final ContentRepository contentRepository;
  private final ReviewQDSLRepository reviewQDSLRepository;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  private void validateAuthor(Review review, UUID authorId, UUID reviewId) {
    if (!review.getAuthorId().equals(authorId)) {
      throw new ReviewException(
          ReviewErrorCode.REVIEW_FORBIDDEN,
          Map.of("reviewId", reviewId)
      );
    }
  }

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

  @Transactional
  public ReviewDto update(UUID reviewId, ReviewUpdateRequest request, ModuPlyUserDetails userDetails) {
    UUID authorId = userDetails.getUserDto().getId();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("reviewId", reviewId)
        ));

    validateAuthor(review, authorId, reviewId);

    review.update(request.text(), request.rating());

    ReviewDto.AuthorDto author = new ReviewDto.AuthorDto(
        authorId,
        userDetails.getUserDto().getName(),
        userDetails.getUserDto().getProfileImageUrl()
    );

    return reviewMapper.toDto(review, author);
  }

  @Transactional
  public void delete(UUID reviewId, ModuPlyUserDetails userDetails) {
    UUID authorId = userDetails.getUserDto().getId();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("reviewId", reviewId)
        ));

    validateAuthor(review, authorId, reviewId);

    reviewRepository.delete(review);
  }

  @Transactional(readOnly = true)
  public CursorResponse<ReviewDto> findAll(ReviewSearchRequest request) {
    List<Review> reviews = reviewQDSLRepository.findAllWithCursor(request);
    long totalCount = reviewQDSLRepository.countWithCondition(request);

    boolean hasNext = reviews.size() > request.limit();

    if (hasNext) {
      reviews = reviews.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      Review last = reviews.get(reviews.size() - 1);
      if (last.getCreatedAt() == null) {
        throw new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("message", "리뷰의 createdAt이 null입니다.")
        );
      }
      if (request.sortBy() == ReviewSortBy.rating) {
        nextCursor = last.getRating() + ":" + last.getCreatedAt().toString();
      } else {
        nextCursor = last.getCreatedAt().toString();
      }
      nextIdAfter = last.getId();
    }

    List<UUID> authorIds = reviews.stream()
        .map(Review::getAuthorId)
        .distinct()
        .toList();

    Map<UUID, UserDto> authorMap = userRepository.findAllById(authorIds)
        .stream()
        .collect(Collectors.toMap(User::getId, userMapper::toDto));

    List<ReviewDto> data = reviews.stream()
        .map(review -> {
          UserDto userDto = authorMap.get(review.getAuthorId());
          ReviewDto.AuthorDto author = new ReviewDto.AuthorDto(
              userDto != null ? userDto.getId() : review.getAuthorId(),
              userDto != null ? userDto.getName() : null,
              userDto != null ? userDto.getProfileImageUrl() : null
          );
          return reviewMapper.toDto(review, author);
        })
        .toList();

    return new CursorResponse<>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );
  }
}
