package com.team6.moduply.review.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.review.dto.ReviewCreateRequest;
import com.team6.moduply.review.dto.ReviewDto;
import com.team6.moduply.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
