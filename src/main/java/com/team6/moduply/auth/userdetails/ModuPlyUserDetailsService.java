package com.team6.moduply.auth.userdetails;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.mapper.UserMapper;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModuPlyUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public ModuPlyUserDetails loadUserByUsername(String username) {
        // 사용자 이름으로 사용자 정보를 조회하는 로직을 구현해야 합니다.
        // 예를 들어, 데이터베이스에서 사용자 정보를 가져오는 코드가 여기에 들어갈 수 있습니다.
        // 현재는 예시로 간단한 사용자 정보를 반환합니다.
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USERNAME_NOT_FOUND_EXCEPTION, Map.of(
                "email" , username
            )));
        UserDto userDto = userMapper.toDto(user);
        return new ModuPlyUserDetails(userDto, user.getEncodedPassword());
    }

}
