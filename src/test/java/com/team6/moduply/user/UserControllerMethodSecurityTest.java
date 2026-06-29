package com.team6.moduply.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.user.controller.UserController;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserLockUpdateRequest;
import com.team6.moduply.user.dto.UserRoleUpdateRequest;
import com.team6.moduply.user.dto.UserUpdateRequest;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserController.class,
    // 이 테스트는 HTTP 보안 필터가 아니라 UserService의 @PreAuthorize를 검증한다.
    // JwtAuthenticationFilter는 별도 auth 테스트에서 검증하므로 Bean 생성 대상에서 제외한다.
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@Import({
    UserService.class,
    UserControllerMethodSecurityTest.MethodSecurityTestConfig.class
})
class UserControllerMethodSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private UserMapper userMapper;

  @MockitoBean
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private BinaryContentService binaryContentService;

  @MockitoBean
  private ApplicationEventPublisher applicationEventPublisher;

  @Test
  @DisplayName("ADMIN 권한으로 사용자 권한 수정 요청 시 204를 반환한다")
  void update_user_role_success_when_requester_is_admin() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
    User user = new User("user@example.com", "encoded-password", "user", Role.USER);
    UserDto response = new UserDto(userId, null, user.getEmail(), user.getName(), null, Role.ADMIN,
        false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(response);

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/role", userId)
            .with(user("admin@example.com").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("USER 권한으로 사용자 권한 수정 요청 시 403을 반환한다")
  void update_user_role_fail_when_requester_is_not_admin() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/role", userId)
            .with(user("user@example.com").roles("USER"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("CSRF 토큰 없이 사용자 권한 수정 요청 시 403을 반환한다")
  void update_user_role_fail_without_csrf_token() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/role", userId)
            .with(user("admin@example.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("ADMIN 권한으로 사용자 계정 잠금 상태 변경 요청 시 204를 반환한다")
  void update_user_locked_success_when_requester_is_admin() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);
    User user = new User("user@example.com", "encoded-password", "user", Role.USER);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/locked", userId)
            .with(user("admin@example.com").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("USER 권한으로 사용자 계정 잠금 상태 변경 요청 시 403을 반환한다")
  void update_user_locked_fail_when_requester_is_not_admin() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/locked", userId)
            .with(user("user@example.com").roles("USER"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("CSRF 토큰 없이 사용자 계정 잠금 상태 변경 요청 시 403을 반환한다")
  void update_user_locked_fail_without_csrf_token() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/locked", userId)
            .with(user("admin@example.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("본인의 프로필 수정 요청 시 200을 반환한다")
  void update_user_success_when_requester_is_owner() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");
    User user = new User("user@example.com", "encoded-password", "user", Role.USER);
    UserDto response = new UserDto(userId, null, user.getEmail(), "updated-name", null, Role.USER,
        false);
    MockMultipartFile requestPart = userUpdateRequestPart(request);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user, null)).willReturn(response);

    // When & Then
    mockMvc.perform(multipart("/api/users/{userId}", userId)
            .file(requestPart)
            .with(httpRequest -> {
              httpRequest.setMethod("PATCH");
              return httpRequest;
            })
            .with(user(userDetails(userId)))
            .with(csrf())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.name").value("updated-name"));

    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user, null);
  }

  @Test
  @DisplayName("다른 사용자의 프로필 수정 요청 시 403을 반환한다")
  void update_user_fail_when_requester_is_not_owner() throws Exception {
    // Given
    UUID requesterId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");
    MockMultipartFile requestPart = userUpdateRequestPart(request);

    // When & Then
    mockMvc.perform(multipart("/api/users/{userId}", targetUserId)
            .file(requestPart)
            .with(httpRequest -> {
              httpRequest.setMethod("PATCH");
              return httpRequest;
            })
            .with(user(userDetails(requesterId)))
            .with(csrf())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("CSRF 토큰 없이 프로필 수정 요청 시 403을 반환한다")
  void update_user_fail_without_csrf_token() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");
    MockMultipartFile requestPart = userUpdateRequestPart(request);

    // When & Then
    mockMvc.perform(multipart("/api/users/{userId}", userId)
            .file(requestPart)
            .with(httpRequest -> {
              httpRequest.setMethod("PATCH");
              return httpRequest;
            })
            .with(user(userDetails(userId)))
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("본인의 비밀번호 변경 요청 시 204를 반환한다")
  void update_user_password_success_when_requester_is_owner() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "old-encoded-password", "user", Role.USER);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.encode("newPassword1!")).willReturn("new-encoded-password");

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/password", userId)
            .with(user(userDetails(userId)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "newPassword": "newPassword1!"
                }
                """))
        .andExpect(status().isNoContent());

    verify(userRepository).findById(userId);
    verify(passwordEncoder).encode("newPassword1!");
  }

  @Test
  @DisplayName("다른 사용자의 비밀번호 변경 요청 시 403을 반환한다")
  void update_user_password_fail_when_requester_is_not_owner() throws Exception {
    // Given
    UUID requesterId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/password", targetUserId)
            .with(user(userDetails(requesterId)))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "newPassword": "newPassword1!"
                }
                """))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  @Test
  @DisplayName("CSRF 토큰 없이 비밀번호 변경 요청 시 403을 반환한다")
  void update_user_password_fail_without_csrf_token() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();

    // When & Then
    mockMvc.perform(patch("/api/users/{userId}/password", userId)
            .with(user(userDetails(userId)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "newPassword": "newPassword1!"
                }
                """))
        .andExpect(status().isForbidden());

    verify(userRepository, never()).findById(any(UUID.class));
  }

  private MockMultipartFile userUpdateRequestPart(UserUpdateRequest request) throws Exception {
    return new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
  }

  private ModuPlyUserDetails userDetails(UUID userId) {
    UserDto userDto = new UserDto(
        userId,
        null,
        "user@example.com",
        "user",
        null,
        Role.USER,
        false
    );
    return new ModuPlyUserDetails(userDto, "encoded-password");
  }

  @EnableMethodSecurity
  static class MethodSecurityTestConfig {
  }
}
