package com.team6.moduply.follow.service;

import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.entity.Follow;
import com.team6.moduply.follow.exception.FollowErrorCode;
import com.team6.moduply.follow.exception.FollowException;
import com.team6.moduply.follow.mapper.FollowMapper;
import com.team6.moduply.follow.repository.FollowRepository;
import com.team6.moduply.notification.event.FollowedEvent;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final FollowMapper followMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public FollowDto createFollow(FollowRequest request, UUID followerId) {
    UUID followeeId = request.followeeId();

    // 팔로우 하는사람
    User follower = findUser(followerId);
    // 팔로우 당하는사람
    User followee = findUser(followeeId);

    // 자기 자신 팔로우 하는지 검증.
    validate(followerId, followeeId);

    try {
      // 팔로우 저장
      Follow saved = followRepository.saveAndFlush(new Follow(follower, followee));
      FollowDto response = followMapper.toDto(saved);
      // 팔로우 이벤트 발행
      eventPublisher.publishEvent(new FollowedEvent(
          followerId,
          follower.getName(),
          followeeId
      ));
      return response;

    } catch (DataIntegrityViolationException e) {
      /// A가 B 팔로우를 동시 요청할시,
      /// existsByFollowerIdAndFolloweeId통과해서 DataIntegrityViolationException 예외일어날 수 있음 방어.
      throw new FollowException(FollowErrorCode.FOLLOW_ALREADY_EXISTS, Map.of("followerId", followerId, "followeeId", followeeId));
    }

  }

  /// 팔로우 취소 메서드.
  @Transactional
  public void cancelFollow(UUID followId, UUID followerId) {
    Follow follow = followRepository.findById(followId)
        .orElseThrow(() -> new FollowException(
            FollowErrorCode.FOLLOW_NOT_FOUND,
            Map.of("followId", followId)
        ));

    // 팔로우 취소하는 사람이 내가 아니라면
    if (!followerId.equals(follow.getFollower().getId())) {
      throw new FollowException(
          FollowErrorCode.FOLLOW_FORBIDDEN,
          Map.of("followId", followId, "followerId", followerId)
      );
    }

    followRepository.delete(follow);
  }

  /// 내가 상대를 팔로우 중인지 확인
  @Transactional(readOnly = true)
  public FollowDto isFollowedByMe(UUID followeeId, UUID followerId) {
    // 나와 상대의 팔로우 관계가 있는지 확인
    Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
        .orElseThrow(() -> new FollowException(
            FollowErrorCode.FOLLOW_NOT_FOUND,
            Map.of("followerId", followerId, "followeeId", followeeId)
        ));

    return followMapper.toDto(follow);
  }

  @Transactional(readOnly = true)
  public long getFollowerCount(UUID followeeId) {
    findUser(followeeId);
    return followRepository.countByFolloweeId(followeeId);
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
