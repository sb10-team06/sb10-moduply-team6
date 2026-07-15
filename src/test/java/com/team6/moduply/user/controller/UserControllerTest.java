package com.team6.moduply.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserFindAllRequest;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.enums.UserSortBy;
import com.team6.moduply.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserService userService;

  @Test
  @DisplayName("회원가입 요청은 201과 생성된 사용자 정보를 반환한다")
  void post_user_returns_created_user() {
    // Given
    UserController userController = new UserController(userService);
    UserCreateRequest request = new UserCreateRequest();
    ReflectionTestUtils.setField(request, "email", "tester@example.com");
    ReflectionTestUtils.setField(request, "password", "Password1!");
    ReflectionTestUtils.setField(request, "name", "tester");
    UserDto userDto = userDto(UUID.randomUUID(), "tester@example.com", "tester", Role.USER);

    given(userService.createUser(request)).willReturn(userDto);

    // When
    ResponseEntity<UserDto> response = userController.postUser(request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(userDto);
    verify(userService).createUser(request);
  }

  @Test
  @DisplayName("사용자 목록 조회 요청은 커서 응답을 반환한다")
  void get_users_returns_cursor_response() {
    // Given
    UserController userController = new UserController(userService);
    UserFindAllRequest request = new UserFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        20,
        SortDirection.ASCENDING,
        UserSortBy.name
    );
    CursorResponse<UserDto> cursorResponse = new CursorResponse<>(
        List.of(userDto(UUID.randomUUID(), "tester@example.com", "tester", Role.USER)),
        null,
        null,
        false,
        1,
        UserSortBy.name.name(),
        SortDirection.ASCENDING
    );
    given(userService.findAll(request)).willReturn(cursorResponse);

    // When
    ResponseEntity<CursorResponse<UserDto>> response = userController.getUsers(request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(cursorResponse);
    verify(userService).findAll(request);
  }

  @Test
  @DisplayName("사용자 단건 조회 요청은 사용자 정보를 반환한다")
  void get_user_returns_user() {
    // Given
    UserController userController = new UserController(userService);
    UUID userId = UUID.randomUUID();
    UserDto userDto = userDto(userId, "tester@example.com", "tester", Role.USER);
    given(userService.getUser(userId)).willReturn(userDto);

    // When
    ResponseEntity<UserDto> response = userController.getUser(userId);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(userDto);
    verify(userService).getUser(userId);
  }

  private UserDto userDto(UUID userId, String email, String name, Role role) {
    return new UserDto(
        userId,
        Instant.now(),
        email,
        name,
        null,
        role,
        false
    );
  }
}
