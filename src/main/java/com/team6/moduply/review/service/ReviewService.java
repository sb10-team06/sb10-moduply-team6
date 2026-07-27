package com.team6.moduply.review.service;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.config.CacheConfig;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.service.ContentService;
import com.team6.moduply.notification.event.FollowActivityEvent;
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
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewQDSLRepository reviewQDSLRepository;
  private final ReviewMapper reviewMapper;
  private final ContentRepository contentRepository;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final BinaryContentService binaryContentService;
  private final ContentService contentService;

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

  /// 조회 결과를 Redis REVIEW_LIST 캐시에 저장.
  /// KEY: 콘텐츠ID + 조회 개수 + 정렬 기준 + 정렬 방향
  /// 첫 페이지 요청만 캐싱.
  @Cacheable(
      cacheNames = CacheConfig.REVIEW_LIST,
      key = "{#request.contentId(), #request.limit(), #request.sortBy(), #request.sortDirection()}",
      condition = "#request.cursor() == null && #request.idAfter() == null",
      sync = true
  )
  @Transactional(readOnly = true)
  public CursorResponse<ReviewDto> findAll(ReviewSearchRequest request) {
    // limit + 1개 조회
    List<Review> reviews = reviewQDSLRepository.findAllWithCursor(request);

    // 전체 리뷰 수 조회: 목록 조회 성능을 위해 실시간 count 쿼리 대신 Content의 비정규화된 reviewCount를 사용한다.
    long totalCount = contentRepository.findById(request.contentId())
        .map(Content::getReviewCount)
        .orElse(0);

    // 다음 페이지 존재 여부
    boolean hasNext = reviews.size() > request.limit();

    // 다음 페이지가 있으면: 추가로 조회한 리뷰 제거
    if (hasNext) {
      reviews = reviews.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;

    // 다음페이지가 있으면: 현재 페이지 마지막 리뷰 cursor에 셋팅
    if (hasNext) {
      Review last = reviews.get(reviews.size() - 1);
      if (last.getCreatedAt() == null) {
        throw new ReviewException(
            ReviewErrorCode.REVIEW_INVALID_STATE,
            Map.of("reviewId", last.getId())
        );
      }
      if (request.sortBy() == ReviewSortBy.rating) {
        nextCursor = last.getRating() + ":" + last.getCreatedAt();
      } else {
        nextCursor = last.getCreatedAt().toString();
      }
      nextIdAfter = last.getId();
    }

    // 작성자 id 추출
    List<UUID> authorIds = reviews.stream()
        .map(Review::getAuthorId)
        .distinct()
        .toList();

    // 작성자 정보 일괄 조회
    Map<UUID, UserDto> authorMap = userRepository.findAllByIdWithProfileImg(authorIds)
        .stream()
        .collect(Collectors.toMap(
            User::getId,
            user -> userMapper.toDto(user, binaryContentService.generateUrl(user.getProfileImg()))
        ));


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
        new ArrayList<>(data),
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );
  }
}
