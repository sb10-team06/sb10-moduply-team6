package com.team6.moduply.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.user.controller.UserController;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserRoleUpdateRequest;
import com.team6.moduply.user.dto.UserUpdateRequest;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
  @DisplayName("다른 사용자의 프로필 수정 요청 시 403을 반환한다")
  void update_user_fail_when_requester_is_not_owner() throws Exception {
    // Given
    UUID requesterId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");
    MockMultipartFile requestPart = new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );

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
