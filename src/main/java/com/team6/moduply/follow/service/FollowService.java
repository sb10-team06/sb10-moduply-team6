package com.team6.moduply.follow.service;

import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.entity.Follow;
import com.team6.moduply.follow.exception.FollowErrorCode;
import com.team6.moduply.follow.exception.FollowException;
import com.team6.moduply.follow.mapper.FollowMapper;
import com.team6.moduply.follow.repository.FollowRepository;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final FollowMapper followMapper;

  @Transactional
  public FollowDto createFollow(FollowRequest request, UUID followerId) {
    UUID followeeId = request.followeeId();

    // 팔로우 하는사람
    User follower = findUser(followerId);
    // 팔로우 당하는사람
    User followee = findUser(followeeId);

    // 자기 자신 팔로우 하는지 검증.
    validate(followerId, followeeId);
    Follow saved;
    try {
      saved = followRepository.save(new Follow(follower, followee));
    } catch (DataIntegrityViolationException e) {
      /// A가 B 팔로우를 동시 요청할시,
      /// existsByFollowerIdAndFolloweeId통과해서 DataIntegrityViolationException 예외일어날 수 있음 방어.
      throw new FollowException(FollowErrorCode.FOLLOW_ALREADY_EXISTS, Map.of("followerId", followerId, "followeeId", followeeId));
    }
    // TODO: SSE용 팔로우 알림 이벤트를 발행한다.

    return followMapper.toDto(saved);
  }


  private void validate(UUID followerId, UUID followeeId) {
    /// 자기자신 팔로우 X
    if (followerId.equals(followeeId)) {
      throw new FollowException(
          FollowErrorCode.SELF_FOLLOW_NOT_ALLOWED,
          Map.of("followerId", followerId, "followeeId", followeeId)
      );
    }

    /// 이미 팔로우하고있는지 검증
    if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
      throw new FollowException(
          FollowErrorCode.FOLLOW_ALREADY_EXISTS,
          Map.of("followerId", followerId, "followeeId", followeeId)
      );
    }
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of("userId", userId)));
  }
}
