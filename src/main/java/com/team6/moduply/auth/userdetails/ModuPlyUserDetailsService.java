package com.team6.moduply.auth.userdetails;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.entity.User;
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
  private final RedisUtil redisUtil;
  private final BinaryContentService binaryContentService;

    @Override
    @Transactional(readOnly = true)
    public ModuPlyUserDetails loadUserByUsername(String username) {
      // redis를 먼저 확인해서 임시 비밀번호가 있는지 확인
      // 없다면 user의 저장된 이메일로 반환

        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new AuthException(AuthErrorCode.USERNAME_NOT_FOUND_EXCEPTION, Map.of(
                "email" , username
            )));
        // TODO: 로그인 인증 과정과 응답용 프로필 URL 생성 책임이 섞여 있으므로 추후 분리 리팩토링 필요
        String profileImageUrl = binaryContentService.generateUrl(user.getProfileImg());
        UserDto userDto = userMapper.toDto(user, profileImageUrl);

        String redisKey = RedisKeyPolicy.PASSWORD_RESET.generateKey(username);
        String encodedPassword = redisUtil.getData(redisKey);

        if(encodedPassword == null){
          encodedPassword = user.getEncodedPassword();
        }
        return new ModuPlyUserDetails(userDto, encodedPassword);
    }

}
