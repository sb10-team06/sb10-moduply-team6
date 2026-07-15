package com.team6.moduply.user.service;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.notification.event.UserRoleUpdatedEvent;
import com.team6.moduply.user.dto.ChangePasswordRequest;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserFindAllRequest;
import com.team6.moduply.user.dto.UserLockUpdateRequest;
import com.team6.moduply.user.dto.UserRoleUpdateRequest;
import com.team6.moduply.user.dto.UserUpdateRequest;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final BinaryContentService binaryContentService;
  private final RedisUtil redisUtil;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public UserDto createUser(UserCreateRequest request){
    log.debug("회원가입 처리 시작");
    if(userRepository.existsByEmail(request.getEmail())){
      log.warn("회원가입 실패: 중복 이메일");

      throw new UserException(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION, Map.of(
          "email", request.getEmail()
      ));
    }
    String encodedPassword = passwordEncoder.encode(request.getPassword());

    User user = new User(request.getEmail(), encodedPassword, request.getName(), Role.USER);
    try {
      userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      log.warn("회원가입 실패: 이메일 유니크 제약 위반", e);

      throw new UserException(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION, Map.of(
          "email", request.getEmail()
      ));
    }

    UserDto response = userMapper.toDto(user);
    log.debug("회원가입 처리 완료. userId={}", response.getId());
    return response;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public CursorResponse<UserDto> findAll(UserFindAllRequest request) {

    // roleEqual, isLocked는 API 확장용 파라미터로 열어두었지만,
    // 현재 프로토타입 UI에서 사용하지 않아 필터 조건에는 아직 반영하지 않는다.
    List<User> users = userRepository.findUsers(
        request.getEmailLike(),
        request.getSortBy(),
        request.getSortDirection(),
        request.getCursor(),
        request.getIdAfter(),
        request.getLimit()
    );

    long totalCount = userRepository.countUsers(request.getEmailLike());

    boolean hasNext = users.size() > request.getLimit();

    if(hasNext){
      users = users.subList(0, request.getLimit());
    }
    List<UserDto> data = users.stream().map(userMapper::toDto).toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if(hasNext){
      User last = users.get(users.size() - 1);
      nextCursor = switch(request.getSortBy()){
        case name -> last.getName();
        case email -> last.getEmail();
        case createdAt -> last.getCreatedAt().toString();
        case isLocked -> String.valueOf(last.isBlocked());
        case role -> last.getRole().toString();
      };
      nextIdAfter = last.getId();
    }

    return new CursorResponse<UserDto>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.getSortBy().name(),
        request.getSortDirection()
    );
  }

  @Transactional(readOnly = true)
  public UserDto getUser(UUID userId) {
    log.debug("사용자 단건 조회 처리 시작. userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of(
            "userId", userId
        )));

    UserDto response = toDto(user);
    log.debug("사용자 단건 조회 처리 완료. userId={}", response.getId());
    return response;
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void updateUserRole(UUID userId, UserRoleUpdateRequest request) {
    log.debug("사용자 권한 변경 처리 시작. userId={}, newRole={}", userId, request.getRole());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of(
            "userId", userId
        )));

    Role oldRole = user.getRole();
    Role newRole = request.getRole();
    if (oldRole == newRole) {
      log.debug("사용자 권한 변경 생략. userId={}, role={}", userId, oldRole);
      return;
    }

    user.updateRole(newRole);
    applicationEventPublisher.publishEvent(new UserRoleUpdatedEvent(userId, oldRole, newRole));
    String email = user.getEmail();

    // 기존 기기의 토큰을 모두 무효화하고 Refresh Token 재발급도 차단한다.
    invalidateToken(email);

    UserDto response = userMapper.toDto(user);
    log.debug("사용자 권한 변경 처리 완료. userId={}, newRole={}", response.getId(), response.getRole());
  }

  @Transactional
  @PreAuthorize("#userId.equals(authentication.principal.userDto.id)")
  public UserDto updateUser(UUID userId, UserUpdateRequest request, MultipartFile profileImg) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of(
            "userId", userId
        )));

    if (StringUtils.hasText(request.getName())) {
      user.updateName(request.getName());
    }

    if (profileImg != null) {
      BinaryContent oldImg = user.getProfileImg();
      try {
        BinaryContent newImg = binaryContentService.createUserProfile(userId, profileImg, oldImg);
        user.updateProfileImg(newImg);
        String profileImageUrl = binaryContentService.generateUrl(newImg);
        return userMapper.toDto(user, profileImageUrl);

      } catch (IOException e) {
        throw new UserException(
            UserErrorCode.PROFILE_IMAGE_UPLOAD_FAILED_EXCEPTION,
            Map.of("reason", "프로필 이미지 업로드에 실패했습니다.")
        );
      }
    }

    String profileImageUrl = binaryContentService.generateUrl(user.getProfileImg());
    return userMapper.toDto(user, profileImageUrl);
  }

  @Transactional
  @PreAuthorize("#userId.equals(authentication.principal.userDto.id)")
  public void updateUserPassword(UUID userId, ChangePasswordRequest request){
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of(
            "userId", userId
        )));

    String newEncodedPassword = passwordEncoder.encode(request.getPassword());
    user.updateEncodedPassword(newEncodedPassword);

    redisUtil.deleteData(RedisKeyPolicy.PASSWORD_RESET.generateKey(user.getEmail()));
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void updateUserLocked(UUID userId, UserLockUpdateRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of(
            "userId", userId
        )));

    user.updateBlocked(request.getLocked());

    String email = user.getEmail();
    if (request.getLocked()) {
      redisUtil.setDataExpire(
          RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(user.getEmail()),
          "locked",
          RedisKeyPolicy.BLACKLIST_LOCKED.getTtl()
      );
      // 잠금 즉시 기존 Access/Refresh Token을 모두 사용할 수 없게 한다.
      invalidateToken(email);
    } else {
      redisUtil.deleteData(RedisKeyPolicy.BLACKLIST_LOCKED.generateKey(email));
    }
  }

  private UserDto toDto(User user) {
    String profileImageUrl = binaryContentService.generateUrl(user.getProfileImg());
    return userMapper.toDto(user, profileImageUrl);
  }

  private void invalidateToken(String email){
    redisUtil.increment(RedisKeyPolicy.USER_TOKEN_VERSION.generateKey(email));
    redisUtil.deleteData(RedisKeyPolicy.REFRESH_TOKEN.generateKey(email));
  }
}
