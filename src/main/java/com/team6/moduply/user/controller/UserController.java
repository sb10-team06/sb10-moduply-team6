package com.team6.moduply.user.controller;

import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
