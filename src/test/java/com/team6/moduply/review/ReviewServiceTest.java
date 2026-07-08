package com.team6.moduply.review;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.repository.ContentRepository;
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
import com.team6.moduply.review.service.ReviewService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @InjectMocks
  private ReviewService reviewService;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewMapper reviewMapper;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ReviewQDSLRepository reviewQDSLRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private ModuPlyUserDetails createUserDetails(UUID userId) {
    UserDto userDto = new UserDto(
        userId, Instant.now(), "test@test.com",
        "테스트", null, Role.USER, false
    );
    return new ModuPlyUserDetails(userDto, "password");
  }

  @Test
  @DisplayName("리뷰를 생성하면 저장된 리뷰를 반환한다.")
  void create_success() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

    Review savedReview = Review.builder()
        .contentId(contentId)
        .authorId(authorId)
        .text("좋아요")
        .rating(4.5)
        .build();

    ReviewDto.AuthorDto authorDto = new ReviewDto.AuthorDto(authorId, "테스트", null);
    ReviewDto expectedDto = new ReviewDto(UUID.randomUUID(), contentId, authorDto, "좋아요", 4.5);

    given(contentRepository.existsById(contentId)).willReturn(true);
    given(reviewRepository.existsByContentIdAndAuthorId(contentId, authorId)).willReturn(false);
    given(reviewRepository.save(any(Review.class))).willReturn(savedReview);
    given(reviewMapper.toDto(any(Review.class), any(ReviewDto.AuthorDto.class))).willReturn(expectedDto);

    // when
    ReviewDto result = reviewService.create(request, userDetails);

    // then
    assertThat(result.text()).isEqualTo("좋아요");
    assertThat(result.rating()).isEqualTo(4.5);
    verify(reviewRepository).save(any(Review.class));
    ArgumentCaptor<FollowActivityEvent> eventCaptor = ArgumentCaptor.forClass(
        FollowActivityEvent.class
    );
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    FollowActivityEvent event = eventCaptor.getValue();
    assertThat(event.getActorId()).isEqualTo(authorId);
    assertThat(event.getActorName()).isEqualTo(userDetails.getUserDto().getName());
    assertThat(event.getActivityContent()).isEqualTo("새 리뷰를 작성했습니다.");
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠에 리뷰를 생성하면 예외가 발생한다.")
  void create_fail_with_not_found_content() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

    given(contentRepository.existsById(contentId)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> reviewService.create(request, userDetails))
        .isInstanceOf(ReviewException.class)
        .satisfies(e -> assertThat(((ReviewException) e).getErrorCode())
            .isEqualTo(ReviewErrorCode.CONTENT_NOT_FOUND));
  }

  @Test
  @DisplayName("이미 리뷰를 작성한 콘텐츠에 다시 리뷰를 생성하면 예외가 발생한다.")
  void create_fail_with_duplicate_review() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

    given(contentRepository.existsById(contentId)).willReturn(true);
    given(reviewRepository.existsByContentIdAndAuthorId(contentId, authorId)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> reviewService.create(request, userDetails))
        .isInstanceOf(ReviewException.class)
        .satisfies(e -> assertThat(((ReviewException) e).getErrorCode())
            .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS));
  }

  @Test
  @DisplayName("작성자가 리뷰를 수정하면 수정된 리뷰를 반환한다.")
  void update_success_with_author() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);

    Review review = Review.builder()
        .contentId(contentId)
        .authorId(authorId)
        .text("원래 내용")
        .rating(4.5)
        .build();

    ReviewDto.AuthorDto authorDto = new ReviewDto.AuthorDto(authorId, "테스트", null);
    ReviewDto expectedDto = new ReviewDto(reviewId, contentId, authorDto, "수정된 내용", 3.0);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(reviewMapper.toDto(any(Review.class), any(ReviewDto.AuthorDto.class))).willReturn(expectedDto);

    // when
    ReviewDto result = reviewService.update(reviewId, request, userDetails);

    // then
    assertThat(review.getText()).isEqualTo("수정된 내용");
    assertThat(review.getRating()).isEqualTo(3.0);
    assertThat(result.text()).isEqualTo("수정된 내용");
    verify(reviewMapper).toDto(any(Review.class), any(ReviewDto.AuthorDto.class));
  }

  @Test
  @DisplayName("존재하지 않는 리뷰를 수정하면 예외가 발생한다.")
  void update_fail_with_not_found_review() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewService.update(reviewId, request, userDetails))
        .isInstanceOf(ReviewException.class)
        .satisfies(e -> assertThat(((ReviewException) e).getErrorCode())
            .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND));
  }

  @Test
  @DisplayName("본인 리뷰가 아닌 리뷰를 수정하면 예외가 발생한다.")
  void update_fail_with_no_permission() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);

    Review review = Review.builder()
        .contentId(contentId)
        .authorId(otherId)
        .text("원래 내용")
        .rating(4.5)
        .build();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    // when & then
    assertThatThrownBy(() -> reviewService.update(reviewId, request, userDetails))
        .isInstanceOf(ReviewException.class)
        .satisfies(e -> assertThat(((ReviewException) e).getErrorCode())
            .isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN));
  }

  @Test
  @DisplayName("작성자가 리뷰를 삭제하면 레포지토리의 delete가 호출된다.")
  void delete_success_with_author() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);

    Review review = Review.builder()
        .contentId(contentId)
        .authorId(authorId)
        .text("내용")
        .rating(4.5)
        .build();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    // when
    reviewService.delete(reviewId, userDetails);

    // then
    verify(reviewRepository).delete(review);
  }

  @Test
  @DisplayName("존재하지 않는 리뷰를 삭제하면 예외가 발생한다.")
  void delete_fail_with_not_found_review() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewService.delete(reviewId, userDetails))
        .isInstanceOf(ReviewException.class)
        .satisfies(e -> assertThat(((ReviewException) e).getErrorCode())
            .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND));
  }

  @Test
  @DisplayName("본인 리뷰가 아닌 리뷰를 삭제하면 예외가 발생한다.")
  void delete_fail_with_no_permission() {
    // given
    UUID authorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ModuPlyUserDetails userDetails = createUserDetails(authorId);

    Review review = Review.builder()
        .contentId(contentId)
        .authorId(otherId)
        .text("내용")
        .rating(4.5)
        .build();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    // when & then
    assertThatThrownBy(() -> reviewService.delete(reviewId, userDetails))
        .isInstanceOf(ReviewException.class)
        .satisfies(e -> assertThat(((ReviewException) e).getErrorCode())
            .isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN));
  }

  @Test
  @DisplayName("리뷰 목록이 limit개이면 hasNext가 false를 반환한다.")
  void findAll_success_with_last_page() {
    // given
    UUID contentId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, null, null, 2, SortDirection.DESCENDING, ReviewSortBy.createdAt
    );

    Review review1 = Review.builder()
        .contentId(contentId).authorId(authorId).text("첫번째").rating(4.0).build();
    Review review2 = Review.builder()
        .contentId(contentId).authorId(authorId).text("두번째").rating(3.5).build();

    ReviewDto.AuthorDto authorDto = new ReviewDto.AuthorDto(authorId, null, null);
    ReviewDto dto1 = new ReviewDto(UUID.randomUUID(), contentId, authorDto, "첫번째", 4.0);
    ReviewDto dto2 = new ReviewDto(UUID.randomUUID(), contentId, authorDto, "두번째", 3.5);

    given(reviewQDSLRepository.findAllWithCursor(request)).willReturn(List.of(review1, review2));
    given(reviewQDSLRepository.countWithCondition(request)).willReturn(2L);
    given(reviewMapper.toDto(any(Review.class), any(ReviewDto.AuthorDto.class)))
        .willReturn(dto1, dto2);
    given(userRepository.findAllById(any())).willReturn(List.of());

    // when
    CursorResponse<ReviewDto> result = reviewService.findAll(request);

    // then
    assertThat(result.data()).hasSize(2);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
  }

  @Test
  @DisplayName("리뷰 목록이 limit개를 초과하면 hasNext가 true이고 nextCursor가 채워진다.")
  void findAll_success_with_has_next_and_cursor() {
    // given
    UUID contentId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    UUID review1Id = UUID.randomUUID();
    Instant review1CreatedAt = Instant.now();

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, null, null, 1, SortDirection.DESCENDING, ReviewSortBy.createdAt
    );

    Review review1 = Review.builder()
        .contentId(contentId).authorId(authorId).text("첫번째").rating(4.0).build();
    Review review2 = Review.builder()
        .contentId(contentId).authorId(authorId).text("두번째").rating(3.5).build();

    ReflectionTestUtils.setField(review1, "id", review1Id);
    ReflectionTestUtils.setField(review1, "createdAt", review1CreatedAt);

    ReviewDto.AuthorDto authorDto = new ReviewDto.AuthorDto(authorId, null, null);
    ReviewDto dto1 = new ReviewDto(review1Id, contentId, authorDto, "첫번째", 4.0);

    given(reviewQDSLRepository.findAllWithCursor(request))
        .willReturn(new ArrayList<>(List.of(review1, review2)));
    given(reviewQDSLRepository.countWithCondition(request)).willReturn(2L);
    given(reviewMapper.toDto(any(Review.class), any(ReviewDto.AuthorDto.class))).willReturn(dto1);
    given(userRepository.findAllById(any())).willReturn(List.of());

    // when
    CursorResponse<ReviewDto> result = reviewService.findAll(request);

    // then
    assertThat(result.data()).hasSize(1);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(review1CreatedAt.toString());
    assertThat(result.nextIdAfter()).isEqualTo(review1Id);
  }

  @Test
  @DisplayName("rating 기준 정렬 시 nextCursor가 rating 값으로 계산된다.")
  void findAll_success_with_rating_sort_cursor() {
    // given
    UUID contentId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    UUID review1Id = UUID.randomUUID();

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, null, null, 1, SortDirection.DESCENDING, ReviewSortBy.rating
    );

    Instant review1CreatedAt = Instant.now();

    Review review1 = Review.builder()
        .contentId(contentId).authorId(authorId).text("첫번째").rating(4.0).build();
    Review review2 = Review.builder()
        .contentId(contentId).authorId(authorId).text("두번째").rating(3.5).build();

    ReflectionTestUtils.setField(review1, "id", review1Id);
    ReflectionTestUtils.setField(review1, "createdAt", review1CreatedAt);

    ReviewDto.AuthorDto authorDto = new ReviewDto.AuthorDto(authorId, null, null);
    ReviewDto dto1 = new ReviewDto(review1Id, contentId, authorDto, "첫번째", 4.0);

    given(reviewQDSLRepository.findAllWithCursor(request))
        .willReturn(new ArrayList<>(List.of(review1, review2)));
    given(reviewQDSLRepository.countWithCondition(request)).willReturn(2L);
    given(reviewMapper.toDto(any(Review.class), any(ReviewDto.AuthorDto.class))).willReturn(dto1);
    given(userRepository.findAllById(any())).willReturn(List.of());

    // when
    CursorResponse<ReviewDto> result = reviewService.findAll(request);

    // then
    assertThat(result.data()).hasSize(1);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(review1.getRating() + ":" + review1CreatedAt.toString());
    assertThat(result.nextIdAfter()).isEqualTo(review1Id);
  }
}
