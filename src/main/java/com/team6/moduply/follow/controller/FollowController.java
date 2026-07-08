package com.team6.moduply.follow.controller;

import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.service.FollowService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/follows")
public class FollowController implements FollowApi {

  private final FollowService followService;

  @PostMapping
  @Override
  public ResponseEntity<FollowDto> createFollow(
      @RequestBody @Valid FollowRequest request,
      @Parameter(hidden = true)
      @AuthenticationPrincipal(expression = "userDto.id") UUID followerId
  ) {

    return ResponseEntity.status(HttpStatus.CREATED).body(followService.createFollow(request, followerId));
  }

  @DeleteMapping("/{followId}")
  @Override
  public ResponseEntity<Void> cancelFollow(
      @PathVariable UUID followId,
      @Parameter(hidden = true)
      @AuthenticationPrincipal(expression = "userDto.id") UUID followerId
  ) {
    // followId: 팔로우 관계자체 고유 id
    // followerId: 팔로우 취소하는 나의 사용자 id
    followService.cancelFollow(followId, followerId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/followed-by-me")
  @Override
  public ResponseEntity<FollowDto> isFollowedByMe(
      // 내가 팔로우 중인지 확인할 대상 사용자ID
      @RequestParam UUID followeeId,
      @Parameter(hidden = true)
      @AuthenticationPrincipal(expression = "userDto.id") UUID followerId
  ) {
    return ResponseEntity.ok(followService.isFollowedByMe(followeeId, followerId));
  }

  @GetMapping("/count")
  @Override
  public ResponseEntity<Long> getFollowerCount(@RequestParam UUID followeeId) {
    return ResponseEntity.ok(followService.getFollowerCount(followeeId));
  }
}
