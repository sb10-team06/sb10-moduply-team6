package com.team6.moduply.review;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.dto.ReviewUpdateRequest;
import com.team6.moduply.review.entity.Review;
import com.team6.moduply.review.exception.ReviewErrorCode;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.mapper.ReviewMapper;
import com.team6.moduply.review.repository.ReviewRepository;
import com.team6.moduply.review.service.ReviewService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
