package com.team6.moduply.follow.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
      @AuthenticationPrincipal(expression = "userDto.id") UUID followerId
  ) {

    return ResponseEntity.status(HttpStatus.CREATED).body(followService.createFollow(request, followerId));
  }
}
