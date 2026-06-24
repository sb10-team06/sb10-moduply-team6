package com.team6.moduply.user.service;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserDto createUser(UserCreateRequest request){
    log.debug("회원가입 처리 시작");
    if(userRepository.existsByEmail(request.getEmail())){
      log.warn("회원가입 실패: 중복 이메일");
      // 추후 커스텀 예외로 변경
      throw new UserException(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION, Map.of(
          "email", request.getEmail()
      ));
    }
    String encodedPassword = passwordEncoder.encode(request.getPassword());
    // 추후 비밀번호 인코딩된 비밀번호로 변경
    User user = new User(request.getEmail(), encodedPassword, request.getName(), Role.USER);
    try {
      userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      log.warn("회원가입 실패: 이메일 유니크 제약 위반", e);
      // 추후 커스텀 예외로 변경
      throw new UserException(UserErrorCode.DUPLICATED_EMAIL_EXCEPTION, Map.of(
          "email", request.getEmail()
      ));
    }

    UserDto response = userMapper.toDto(user);
    log.debug("회원가입 처리 완료. userId={}", response.getId());
    return response;
  }
}
