package com.team6.moduply.user.controller;

import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserRoleUpdateRequest;
import com.team6.moduply.user.dto.UserUpdateRequest;
import com.team6.moduply.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {
  private final UserService userService;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public ResponseEntity<UserDto> postUser(@Valid @RequestBody UserCreateRequest request) {
    log.info("회원가입 요청 수신");
    UserDto response = userService.createUser(request);
    log.info("회원가입 요청 처리 완료. userId={}", response.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{userId}")
  @Override
  public ResponseEntity<UserDto> getUser(@PathVariable UUID userId) {
    log.info("사용자 단건 조회 요청 수신. userId={}", userId);
    UserDto user = userService.getUser(userId);
    log.info("사용자 단건 조회 요청 처리 완료. userId={}", user.getId());
    return ResponseEntity.ok(user);
  }

  @PatchMapping("/{userId}/role")
  @Override
  public ResponseEntity<Void> updateUserRole(
      @PathVariable UUID userId,
      @Valid @RequestBody UserRoleUpdateRequest request
  ) {
    log.info("사용자 권한 변경 요청 수신. userId={}, role={}", userId, request.getRole());
    userService.updateUserRole(userId, request);
    log.info("사용자 권한 변경 요청 처리 완료. userId={}", userId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Override
  public ResponseEntity<UserDto> updateUser(
      @PathVariable UUID userId,
      @Valid @RequestPart("request") UserUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile profileImg
  ) {
    log.info("사용자 정보 수정 요청 수신. userId={}", userId);
    UserDto response = userService.updateUser(userId, request, profileImg);
    log.info("사용자 정보 수정 요청 처리 완료. userId={}", response.getId());

    return ResponseEntity.ok(response);
  }
}
