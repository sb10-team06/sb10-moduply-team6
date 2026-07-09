package com.team6.moduply.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.review.controller.ReviewController;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.dto.ReviewUpdateRequest;
import com.team6.moduply.review.exception.ReviewErrorCode;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.service.ReviewService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ReviewController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ReviewService reviewService;

  private static final UUID TEST_AUTHOR_ID = UUID.randomUUID();

  private ModuPlyUserDetails createUserDetails() {
    UserDto userDto = new UserDto(
        TEST_AUTHOR_ID, Instant.now(), "test@test.com",
        "테스트", null, Role.USER, false
    );
    return new ModuPlyUserDetails(userDto, "password");
  }

  @Test
  @DisplayName("리뷰를 생성하면 201을 반환한다.")
  void createReview_success() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

    ReviewDto.AuthorDto authorDto = new ReviewDto.AuthorDto(TEST_AUTHOR_ID, "테스트", null);
    ReviewDto response = new ReviewDto(UUID.randomUUID(), contentId, authorDto, "좋아요", 4.5);

    given(reviewService.create(any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.text").value("좋아요"))
        .andExpect(jsonPath("$.rating").value(4.5));
  }

  @Test
  @DisplayName("콘텐츠 없이 리뷰를 생성하면 400을 반환한다.")
  void createReview_fail_with_no_content_id() throws Exception {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(null, "좋아요", 4.5);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠에 리뷰를 생성하면 404를 반환한다.")
  void createReview_fail_with_not_found_content() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

    given(reviewService.create(any(), any()))
        .willThrow(new ReviewException(
            ReviewErrorCode.CONTENT_NOT_FOUND,
            Map.of("contentId", contentId)
        ));

    // when & then
    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("이미 리뷰를 작성한 콘텐츠에 다시 리뷰를 생성하면 409를 반환한다.")
  void createReview_fail_with_duplicate() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

    given(reviewService.create(any(), any()))
        .willThrow(new ReviewException(
            ReviewErrorCode.REVIEW_ALREADY_EXISTS,
            Map.of("contentId", contentId)
        ));

    // when & then
    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("리뷰를 수정하면 200을 반환한다.")
  void updateReview_success() throws Exception {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);

    ReviewDto.AuthorDto authorDto = new ReviewDto.AuthorDto(TEST_AUTHOR_ID, "테스트", null);
    ReviewDto response = new ReviewDto(reviewId, contentId, authorDto, "수정된 내용", 3.0);

    given(reviewService.update(any(), any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/reviews/" + reviewId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.text").value("수정된 내용"))
        .andExpect(jsonPath("$.rating").value(3.0));

    verify(reviewService).update(eq(reviewId), any(ReviewUpdateRequest.class), any());
  }

  @Test
  @DisplayName("존재하지 않는 리뷰를 수정하면 404를 반환한다.")
  void updateReview_fail_with_not_found() throws Exception {
    // given
    UUID reviewId = UUID.randomUUID();
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);

    given(reviewService.update(any(), any(), any()))
        .willThrow(new ReviewException(
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("reviewId", reviewId)
        ));

    // when & then
    mockMvc.perform(patch("/api/reviews/" + reviewId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("본인 리뷰가 아닌 리뷰를 수정하면 403을 반환한다.")
  void updateReview_fail_with_forbidden() throws Exception {
    // given
    UUID reviewId = UUID.randomUUID();
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 3.0);

    given(reviewService.update(any(), any(), any()))
        .willThrow(new ReviewException(
            ReviewErrorCode.REVIEW_FORBIDDEN,
            Map.of("reviewId", reviewId)
        ));

    // when & then
    mockMvc.perform(patch("/api/reviews/" + reviewId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("리뷰를 삭제하면 204를 반환한다.")
  void deleteReview_success() throws Exception {
    // given
    UUID reviewId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/reviews/" + reviewId))
        .andExpect(status().isNoContent());

    verify(reviewService).delete(eq(reviewId), any());
  }

  @Test
  @DisplayName("존재하지 않는 리뷰를 삭제하면 404를 반환한다.")
  void deleteReview_fail_with_not_found() throws Exception {
    // given
    UUID reviewId = UUID.randomUUID();

    doThrow(new ReviewException(
        ReviewErrorCode.REVIEW_NOT_FOUND,
        Map.of("reviewId", reviewId)
    )).when(reviewService).delete(any(), any());

    // when & then
    mockMvc.perform(delete("/api/reviews/" + reviewId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("본인 리뷰가 아닌 리뷰를 삭제하면 403을 반환한다.")
  void deleteReview_fail_with_forbidden() throws Exception {
    // given
    UUID reviewId = UUID.randomUUID();

    doThrow(new ReviewException(
        ReviewErrorCode.REVIEW_FORBIDDEN,
        Map.of("reviewId", reviewId)
    )).when(reviewService).delete(any(), any());

    // when & then
    mockMvc.perform(delete("/api/reviews/" + reviewId))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("리뷰 목록을 조회하면 200을 반환한다.")
  void getReviews_success() throws Exception {
    // given
    CursorResponse<ReviewDto> response = new CursorResponse<>(
        List.of(), null, null, false, 0L, "createdAt", SortDirection.DESCENDING
    );

    given(reviewService.findAll(any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/reviews")
            .param("contentId", UUID.randomUUID().toString())
            .param("limit", "10")
            .param("sortDirection", "DESCENDING")
            .param("sortBy", "createdAt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(0));

    verify(reviewService).findAll(any(ReviewSearchRequest.class));
  }
}