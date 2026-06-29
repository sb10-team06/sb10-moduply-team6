package com.team6.moduply.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.entity.Follow;
import com.team6.moduply.follow.exception.FollowErrorCode;
import com.team6.moduply.follow.exception.FollowException;
import com.team6.moduply.follow.mapper.FollowMapper;
import com.team6.moduply.follow.repository.FollowRepository;
import com.team6.moduply.follow.service.FollowService;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

  @InjectMocks
  private FollowService followService;

  @Mock
  private FollowRepository followRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private FollowMapper followMapper;

  @Test
  @DisplayName("다른 사용자를 팔로우하면 팔로우 관계를 저장하고 응답을 반환한다.")
  void createFollow_success_with_valid_users() {
    // given
    // 팔로우 하는사람
    UUID followerId = UUID.randomUUID();
    // 팔로우 당하는사람
    UUID followeeId = UUID.randomUUID();
    FollowRequest request = new FollowRequest(followeeId);
    User follower = new User("follower@example.com", "password", "follower", Role.USER);
    User followee = new User("followee@example.com", "password", "followee", Role.USER);
    // 팔로우 관계 저장
    Follow savedFollow = new Follow(follower, followee);
    // 예상 응답DTO
    FollowDto expected = new FollowDto(UUID.randomUUID(), followerId, followeeId);
    // follower, followee 둘다 존재.
    given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
    given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
    // 팔로우 관계 아니다.
    given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(false);
    given(followRepository.saveAndFlush(any(Follow.class))).willReturn(savedFollow);
    given(followMapper.toDto(savedFollow)).willReturn(expected);

    // when
    FollowDto result = followService.createFollow(request, followerId);

    // then
    // 응답DTO 같은지?
    assertThat(result).isEqualTo(expected);

    ArgumentCaptor<Follow> followCaptor = ArgumentCaptor.forClass(Follow.class);
    verify(followRepository).saveAndFlush(followCaptor.capture());
    // follow 객체
    Follow capturedFollow = followCaptor.getValue();
    // 팔로우하는사람, 당하는사람이 맞게 저장됐는지?
    assertThat(capturedFollow.getFollower()).isEqualTo(follower);
    assertThat(capturedFollow.getFollowee()).isEqualTo(followee);

    verify(followMapper).toDto(savedFollow);
  }

  @Test
  @DisplayName("자기 자신을 팔로우하면 예외가 발생한다.")
  void createFollow_fail_when_following_self() {
    // given
    UUID userId = UUID.randomUUID();
    FollowRequest request = new FollowRequest(userId);
    User user = new User("user@example.com", "password", "user", Role.USER);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when & then
    assertThatThrownBy(() -> followService.createFollow(request, userId))
        .isInstanceOfSatisfying(FollowException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(FollowErrorCode.SELF_FOLLOW_NOT_ALLOWED);
          assertThat(exception.getDetails().get("followerId")).isEqualTo(userId);
          assertThat(exception.getDetails().get("followeeId")).isEqualTo(userId);
        });

    verify(followRepository, never()).saveAndFlush(any(Follow.class));
    verify(followMapper, never()).toDto(any(Follow.class));
  }

  @Test
  @DisplayName("이미 팔로우한 사용자라면 예외가 발생한다.")
  void createFollow_fail_when_follow_already_exists() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowRequest request = new FollowRequest(followeeId);
    User follower = new User("follower@example.com", "password", "follower", Role.USER);
    User followee = new User("followee@example.com", "password", "followee", Role.USER);

    given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
    given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
    given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> followService.createFollow(request, followerId))
        .isInstanceOfSatisfying(FollowException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(FollowErrorCode.FOLLOW_ALREADY_EXISTS);
          assertThat(exception.getDetails().get("followerId")).isEqualTo(followerId);
          assertThat(exception.getDetails().get("followeeId")).isEqualTo(followeeId);
        });

    verify(followRepository, never()).saveAndFlush(any(Follow.class));
    verify(followMapper, never()).toDto(any(Follow.class));
  }

  @Test
  @DisplayName("팔로우 요청자가 존재하지 않으면 예외가 발생한다.")
  void createFollow_fail_when_follower_not_found() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowRequest request = new FollowRequest(followeeId);

    given(userRepository.findById(followerId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.createFollow(request, followerId))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(followerId);
        });

    verify(followRepository, never()).existsByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class));
    verify(followRepository, never()).saveAndFlush(any(Follow.class));
    verify(followMapper, never()).toDto(any(Follow.class));
  }

  @Test
  @DisplayName("팔로우 대상자가 존재하지 않으면 예외가 발생한다.")
  void createFollow_fail_when_followee_not_found() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowRequest request = new FollowRequest(followeeId);
    User follower = new User("follower@example.com", "password", "follower", Role.USER);

    given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
    given(userRepository.findById(followeeId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.createFollow(request, followerId))
        .isInstanceOfSatisfying(UserException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_EXCEPTION);
          assertThat(exception.getDetails().get("userId")).isEqualTo(followeeId);
        });

    verify(followRepository, never()).existsByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class));
    verify(followRepository, never()).saveAndFlush(any(Follow.class));
    verify(followMapper, never()).toDto(any(Follow.class));
  }

  @Test
  @DisplayName("본인이 생성한 팔로우를 취소하면 팔로우 관계를 삭제한다.")
  void cancelFollow_success_with_owner() {
    // given
    UUID followId = UUID.randomUUID();
    UUID followerId = UUID.randomUUID();
    User follower = new User("follower@example.com", "password", "follower", Role.USER);
    User followee = new User("followee@example.com", "password", "followee", Role.USER);
    ReflectionTestUtils.setField(follower, "id", followerId);
    Follow follow = new Follow(follower, followee);

    given(followRepository.findById(followId)).willReturn(Optional.of(follow));

    // when
    followService.cancelFollow(followId, followerId);

    // then
    verify(followRepository).delete(follow);
  }

  @Test
  @DisplayName("존재하지 않는 팔로우를 취소하면 예외가 발생한다.")
  void cancelFollow_fail_when_not_found() {
    // given
    UUID followId = UUID.randomUUID();
    UUID followerId = UUID.randomUUID();

    given(followRepository.findById(followId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.cancelFollow(followId, followerId))
        .isInstanceOfSatisfying(FollowException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(FollowErrorCode.FOLLOW_NOT_FOUND);
          assertThat(exception.getDetails().get("followId")).isEqualTo(followId);
        });

    verify(followRepository, never()).delete(any(Follow.class));
  }

  @Test
  @DisplayName("다른 사용자의 팔로우를 취소하면 예외가 발생한다.")
  void cancelFollow_fail_with_invalid_owner() {
    // given
    UUID followId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    User follower = new User("follower@example.com", "password", "follower", Role.USER);
    User followee = new User("followee@example.com", "password", "followee", Role.USER);
    ReflectionTestUtils.setField(follower, "id", ownerId);
    Follow follow = new Follow(follower, followee);

    given(followRepository.findById(followId)).willReturn(Optional.of(follow));

    // when & then
    assertThatThrownBy(() -> followService.cancelFollow(followId, requesterId))
        .isInstanceOfSatisfying(FollowException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(FollowErrorCode.FOLLOW_FORBIDDEN);
          assertThat(exception.getDetails().get("followId")).isEqualTo(followId);
          assertThat(exception.getDetails().get("followerId")).isEqualTo(requesterId);
        });

    verify(followRepository, never()).delete(any(Follow.class));
  }

  @Test
  @DisplayName("특정 사용자를 내가 팔로우 중이면 팔로우 정보를 반환한다.")
  void isFollowedByMe_success_when_follow_exists() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    User follower = new User("follower@example.com", "password", "follower", Role.USER);
    User followee = new User("followee@example.com", "password", "followee", Role.USER);
    Follow follow = new Follow(follower, followee);
    FollowDto expected = new FollowDto(UUID.randomUUID(), followerId, followeeId);

    given(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(Optional.of(follow));
    given(followMapper.toDto(follow)).willReturn(expected);

    // when
    FollowDto result = followService.isFollowedByMe(followeeId, followerId);

    // then
    assertThat(result).isEqualTo(expected);
    verify(followMapper).toDto(follow);
  }

  @Test
  @DisplayName("특정 사용자를 내가 팔로우하지 않으면 예외가 발생한다.")
  void isFollowedByMe_fail_when_follow_not_found() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();

    given(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.isFollowedByMe(followeeId, followerId))
        .isInstanceOfSatisfying(FollowException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(FollowErrorCode.FOLLOW_NOT_FOUND);
          assertThat(exception.getDetails().get("followerId")).isEqualTo(followerId);
          assertThat(exception.getDetails().get("followeeId")).isEqualTo(followeeId);
        });

    verify(followMapper, never()).toDto(any(Follow.class));
  }
}
