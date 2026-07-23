package com.team6.moduply.review.service;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.config.CacheConfig;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.dto.ReviewSortBy;
import com.team6.moduply.review.entity.Review;
import com.team6.moduply.review.exception.ReviewErrorCode;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.mapper.ReviewMapper;
import com.team6.moduply.review.repository.qdsl.ReviewQDSLRepository;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewListCacheService {

  private final ContentRepository contentRepository;
  private final ReviewQDSLRepository reviewQDSLRepository;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ReviewMapper reviewMapper;
  private final BinaryContentService binaryContentService;

  @Cacheable(
      cacheNames = CacheConfig.REVIEW_LIST,
      key = "{#request.contentId(), #request.limit(), #request.sortBy(), #request.sortDirection()}",
      condition = "#request.cursor() == null && #request.idAfter() == null",
      sync = true
  )
  @Transactional(readOnly = true)
  public CursorResponse<ReviewDto> findAll(ReviewSearchRequest request) {
    List<Review> reviews = reviewQDSLRepository.findAllWithCursor(request);
    // 목록 조회 성능을 위해 실시간 count 쿼리 대신 Content의 비정규화된 reviewCount를 사용한다.
    long totalCount = contentRepository.findById(request.contentId())
        .map(Content::getReviewCount)
        .orElse(0);

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

    List<UUID> authorIds = reviews.stream()
        .map(Review::getAuthorId)
        .distinct()
        .toList();

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
