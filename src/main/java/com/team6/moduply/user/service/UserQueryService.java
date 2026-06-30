package com.team6.moduply.user.service;

import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.dto.UserSummary;
import com.team6.moduply.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserQueryService {

  private final UserService userService;

  public UserSummary findById(UUID id) {
    UserDto userDto = userService.getUser(id);
    return new UserSummary(
        userDto.getId(),
        userDto.getName(),
        userDto.getProfileImageUrl()
    );
  }


}
