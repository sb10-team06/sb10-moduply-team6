package com.team6.moduply.review.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.dto.ReviewSearchRequest;
import com.team6.moduply.review.dto.ReviewUpdateRequest;
import com.team6.moduply.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "리뷰 관리")
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping
  @Operation(summary = "리뷰 생성", description = "생성한 리뷰는 API 요청자 본인의 리뷰로 생성됩니다.")
  public ResponseEntity<ReviewDto> createReview(
      @RequestBody @Valid ReviewCreateRequest request,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    return ResponseEntity.status(201).body(reviewService.create(request, userDetails));
  }

  @PatchMapping("/{reviewId}")
  @Operation(summary = "리뷰 수정", description = "리뷰를 수정합니다.")
  public ResponseEntity<ReviewDto> updateReview(
      @PathVariable UUID reviewId,
      @RequestBody @Valid ReviewUpdateRequest request,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    return ResponseEntity.ok(reviewService.update(reviewId, request, userDetails));
  }

  @DeleteMapping("/{reviewId}")
  @Operation(summary = "리뷰 삭제", description = "리뷰를 삭제합니다.")
  public ResponseEntity<Void> deleteReview(
      @PathVariable UUID reviewId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    reviewService.delete(reviewId, userDetails);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @Operation(summary = "리뷰 목록 조회", description = "특정 콘텐츠의 리뷰 목록을 커서 기반 페이지네이션으로 조회합니다.")
  public ResponseEntity<CursorResponse<ReviewDto>> getReviews(
      @ModelAttribute @Valid ReviewSearchRequest request) {
    return ResponseEntity.ok(reviewService.findAll(request));
  }
}
