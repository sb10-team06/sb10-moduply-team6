package com.team6.moduply.follow.controller;

import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
      @RequestHeader("Monew-Request-User-Id") UUID followerId
  ) {
    // TODO: 현재 임시 요청 헤더를 사용 중이므로, 추후 JWT에서 사용자 ID를 추출하도록 변경한다.
    return ResponseEntity.status(HttpStatus.CREATED).body(followService.createFollow(request, followerId));
  }
}
