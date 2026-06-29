package com.team6.moduply.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.review.controller.ReviewController;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.exception.ReviewErrorCode;
import com.team6.moduply.review.exception.ReviewException;
import com.team6.moduply.review.service.ReviewService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
            .with(user(createUserDetails()))
            .with(csrf())
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
            .with(user(createUserDetails()))
            .with(csrf())
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
            ReviewErrorCode.REVIEW_NOT_FOUND,
            Map.of("contentId", contentId)
        ));

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(user(createUserDetails()))
            .with(csrf())
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
            .with(user(createUserDetails()))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("인증 없이 리뷰를 생성하면 401을 반환한다.")
  void createReview_fail_with_no_auth() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

    // when & then
    mockMvc.perform(post("/api/reviews")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
