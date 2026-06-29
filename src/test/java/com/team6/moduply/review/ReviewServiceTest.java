package com.team6.moduply.review;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.entity.Review;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.mapper.ReviewMapper;
import com.team6.moduply.review.repository.ReviewRepository;
import com.team6.moduply.review.service.ReviewService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
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
        .isInstanceOf(ReviewException.class);
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
        .isInstanceOf(ReviewException.class);
  }
}
