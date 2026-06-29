package com.team6.moduply.user;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.user.dto.ChangePasswordRequest;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserLockUpdateRequest;
import com.team6.moduply.user.dto.UserRoleUpdateRequest;
import com.team6.moduply.user.dto.UserUpdateRequest;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.event.PasswordChangeEvent;
import com.team6.moduply.user.event.UserLockedEvent;
import com.team6.moduply.user.event.UserUnlockedEvent;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private BinaryContentService binaryContentService;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("비밀번호를 인코딩하여 회원가입 성공")
  public void sign_up_success_with_encoded_password() {
    // Given
    UserCreateRequest request = new UserCreateRequest("tester","test@example.com", "password1!");
    User savedUser = new User("test@example.com", "encoded-password", "tester", Role.USER);
    UserDto expected = new UserDto(null,null,savedUser.getEmail(), savedUser.getName(), null, Role.USER, false);

    given(userRepository.existsByEmail(savedUser.getEmail())).willReturn(false);
    given(userMapper.toDto(any(User.class))).willReturn(expected);
    given(passwordEncoder.encode(request.getPassword())).willReturn(savedUser.getEncodedPassword());
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // When
    UserDto response = userService.createUser(request);

    // Then
    assertThat(response.getEmail()).isEqualTo(expected.getEmail());
    assertThat(response.getRole()).isEqualTo(expected.getRole());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User capturedUser = userCaptor.getValue();
    assertThat(capturedUser.getEmail()).isEqualTo(request.getEmail());
    assertThat(capturedUser.getName()).isEqualTo(request.getName());
    assertThat(capturedUser.getRole()).isEqualTo(Role.USER);
    assertThat(capturedUser.getEncodedPassword()).isEqualTo("encoded-password");
    assertThat(capturedUser.getEncodedPassword()).isNotEqualTo(request.getPassword());

    verify(passwordEncoder).encode(request.getPassword());
  }

  @Test
  @DisplayName("이미 존재하는 이메일이면 회원가입 실패")
  public void sign_up_fail_when_email_already_exists() {
    // Given
    UserCreateRequest request = new UserCreateRequest("tester", "test@example.com", "password1!");

    given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

    // When & Then
    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION);
          assertThat(exception.getDetails().get("email")).isEqualTo(request.getEmail());
        });

    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("동시 회원가입으로 DB 유니크 제약 위반 시 회원가입 실패")
  public void sign_up_fail_when_email_unique_constraint_violated() {
    // Given
    UserCreateRequest request = new UserCreateRequest("tester", "test@example.com", "password1!");

    given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
    given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-password");
    given(userRepository.save(any(User.class)))
        .willThrow(new DataIntegrityViolationException("duplicate email"));

    // When & Then
    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION);
          assertThat(exception.getDetails().get("email")).isEqualTo(request.getEmail());
        });

    verify(passwordEncoder).encode(request.getPassword());
    verify(userRepository).save(any(User.class));
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("사용자 단건 조회 성공")
  public void get_user_success() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);
    UserDto expected = new UserDto(userId, null, user.getEmail(), user.getName(), null, Role.USER,
        false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(expected);

    // When
    UserDto response = userService.getUser(userId);

    // Then
    assertThat(response.getId()).isEqualTo(expected.getId());
    assertThat(response.getEmail()).isEqualTo(expected.getEmail());
    assertThat(response.getName()).isEqualTo(expected.getName());
    assertThat(response.getRole()).isEqualTo(expected.getRole());

    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 단건 조회 실패")
  public void get_user_fail_when_user_not_found() {
    // Given
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.getUser(userId))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository).findById(userId);
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("사용자 권한 수정 성공")
  public void update_user_role_success() {
    // Given
    UUID userId = UUID.randomUUID();
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);
    UserDto expected = new UserDto(userId, null, user.getEmail(), user.getName(), null, Role.ADMIN,
        false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(expected);

    // When
    userService.updateUserRole(userId, request);

    // Then
    assertThat(user.getRole()).isEqualTo(Role.ADMIN);

    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 권한 수정 실패")
  public void update_user_role_fail_when_user_not_found() {
    // Given
    UUID userId = UUID.randomUUID();
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.updateUserRole(userId, request))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository).findById(userId);
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("사용자 이름만 수정 성공")
  public void update_user_success_with_name_only() throws IOException {
    // Given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);
    UserDto expected = new UserDto(userId, null, user.getEmail(), "updated-name", null, Role.USER,
        false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user, null)).willReturn(expected);

    // When
    UserDto response = userService.updateUser(userId, request, null);

    // Then
    assertThat(response.getId()).isEqualTo(expected.getId());
    assertThat(response.getName()).isEqualTo("updated-name");
    assertThat(user.getName()).isEqualTo("updated-name");

    verify(userRepository).findById(userId);
    verify(binaryContentService, never())
        .createUserProfile(any(UUID.class), any(MultipartFile.class), any());
    verify(userMapper).toDto(user, null);
  }

  @Test
  @DisplayName("사용자 이름과 프로필 이미지 수정 성공")
  public void update_user_success_with_profile_image() throws IOException {
    // Given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);
    MockMultipartFile profileImg = new MockMultipartFile(
        "image",
        "profile.png",
        "image/png",
        "image".getBytes()
    );
    BinaryContent newProfileImg = BinaryContent.create(
        "profile.png",
        profileImg.getSize(),
        profileImg.getContentType(),
        "users/" + userId + "/profile.png"
    );
    String profileImageUrl = "https://cdn.example.com/users/" + userId + "/profile.png";
    UserDto expected = new UserDto(userId, null, user.getEmail(), "updated-name",
        profileImageUrl, Role.USER, false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(binaryContentService.createUserProfile(any(UUID.class), any(MultipartFile.class), any()))
        .willReturn(newProfileImg);
    given(binaryContentService.generateUrl(newProfileImg)).willReturn(profileImageUrl);
    given(userMapper.toDto(user, profileImageUrl)).willReturn(expected);

    // When
    UserDto response = userService.updateUser(userId, request, profileImg);

    // Then
    assertThat(response.getName()).isEqualTo("updated-name");
    assertThat(response.getProfileImageUrl()).isEqualTo(profileImageUrl);
    assertThat(user.getProfileImg()).isEqualTo(newProfileImg);

    verify(userRepository).findById(userId);
    verify(binaryContentService).createUserProfile(userId, profileImg, null);
    verify(binaryContentService).generateUrl(newProfileImg);
    verify(userMapper).toDto(user, profileImageUrl);
  }

  @Test
  @DisplayName("프로필 이미지 업로드 중 IOException 발생 시 사용자 수정 실패")
  public void update_user_fail_when_profile_image_upload_failed() throws IOException {
    // Given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);
    MockMultipartFile profileImg = new MockMultipartFile(
        "image",
        "profile.png",
        "image/png",
        "image".getBytes()
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(binaryContentService.createUserProfile(any(UUID.class), any(MultipartFile.class), any()))
        .willThrow(new IOException("upload failed"));

    // When & Then
    assertThatThrownBy(() -> userService.updateUser(userId, request, profileImg))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode())
              .isEqualTo(UserErrorCode.PROFILE_IMAGE_UPLOAD_FAILED_EXCEPTION);
          assertThat(exception.getDetails().get("reason")).isEqualTo("프로필 이미지 업로드에 실패했습니다.");
        });

    verify(userRepository).findById(userId);
    verify(binaryContentService).createUserProfile(userId, profileImg, null);
    verify(binaryContentService, never()).generateUrl(any(BinaryContent.class));
    verify(userMapper, never()).toDto(any(User.class), any());
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 사용자 수정 실패")
  public void update_user_fail_when_user_not_found() throws IOException {
    // Given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("updated-name");

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.updateUser(userId, request, null))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository).findById(userId);
    verify(binaryContentService, never())
        .createUserProfile(any(UUID.class), any(MultipartFile.class), any());
    verify(userMapper, never()).toDto(any(User.class), any());
  }

  @Test
  @DisplayName("비밀번호 변경 성공 시 새 비밀번호를 인코딩하고 비밀번호 변경 이벤트를 발행한다")
  public void update_user_password_success() {
    // Given
    UUID userId = UUID.randomUUID();
    ChangePasswordRequest request = changePasswordRequest("newPassword1!");
    User user = new User("test@example.com", "old-encoded-password", "tester", Role.USER);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(passwordEncoder.encode(request.getNewPassword())).willReturn("new-encoded-password");

    // When
    userService.updateUserPassword(userId, request);

    // Then
    assertThat(user.getEncodedPassword()).isEqualTo("new-encoded-password");

    verify(userRepository).findById(userId);
    verify(passwordEncoder).encode(request.getNewPassword());

    ArgumentCaptor<PasswordChangeEvent> eventCaptor =
        ArgumentCaptor.forClass(PasswordChangeEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEmail()).isEqualTo(user.getEmail());
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 비밀번호 변경 실패")
  public void update_user_password_fail_when_user_not_found() {
    // Given
    UUID userId = UUID.randomUUID();
    ChangePasswordRequest request = changePasswordRequest("newPassword1!");

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.updateUserPassword(userId, request))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository).findById(userId);
    verify(passwordEncoder, never()).encode(anyString());
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("사용자 계정 잠금 성공 시 잠금 상태를 변경하고 잠금 이벤트를 발행한다")
  public void update_user_locked_success_when_locked_true() {
    // Given
    UUID userId = UUID.randomUUID();
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // When
    userService.updateUserLocked(userId, request);

    // Then
    assertThat(user.isBlocked()).isTrue();

    verify(userRepository).findById(userId);

    ArgumentCaptor<UserLockedEvent> eventCaptor =
        ArgumentCaptor.forClass(UserLockedEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEmail()).isEqualTo(user.getEmail());
  }

  @Test
  @DisplayName("사용자 계정 잠금 해제 성공 시 잠금 상태를 해제하고 잠금 해제 이벤트를 발행한다")
  public void update_user_locked_success_when_locked_false() {
    // Given
    UUID userId = UUID.randomUUID();
    UserLockUpdateRequest request = new UserLockUpdateRequest(false);
    User user = new User("test@example.com", "encoded-password", "tester", Role.USER);
    user.updateBlocked(true);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // When
    userService.updateUserLocked(userId, request);

    // Then
    assertThat(user.isBlocked()).isFalse();

    verify(userRepository).findById(userId);

    ArgumentCaptor<UserUnlockedEvent> eventCaptor =
        ArgumentCaptor.forClass(UserUnlockedEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEmail()).isEqualTo(user.getEmail());
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 계정 잠금 상태 변경 실패")
  public void update_user_locked_fail_when_user_not_found() {
    // Given
    UUID userId = UUID.randomUUID();
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> userService.updateUserLocked(userId, request))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
        });

    verify(userRepository).findById(userId);
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  private ChangePasswordRequest changePasswordRequest(String newPassword) {
    ChangePasswordRequest request = new ChangePasswordRequest();
    ReflectionTestUtils.setField(request, "newPassword", newPassword);
    return request;
  }

}
