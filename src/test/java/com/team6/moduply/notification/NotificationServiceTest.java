package com.team6.moduply.notification;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.dto.NotificationSearchRequest;
import com.team6.moduply.notification.dto.NotificationSortBy;
import com.team6.moduply.notification.entity.Notification;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.enums.NotificationType;
import com.team6.moduply.notification.exception.NotificationErrorCode;
import com.team6.moduply.notification.exception.NotificationException;
import com.team6.moduply.notification.mapper.NotificationMapper;
import com.team6.moduply.notification.repository.NotificationRepository;
import com.team6.moduply.notification.repository.qdsl.NotificationQDSLRepository;
import com.team6.moduply.notification.service.NotificationService;
import com.team6.moduply.user.enums.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @InjectMocks
  private NotificationService notificationService;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private NotificationQDSLRepository notificationQDSLRepository;

  @Mock
  private NotificationMapper notificationMapper;

  @Test
  @DisplayName("알림 목록을 조회하면 커서 응답을 반환한다.")
  void findAll_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    NotificationSearchRequest request = new NotificationSearchRequest(
        null, null, 10, SortDirection.DESCENDING, NotificationSortBy.createdAt
    );

    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title("제목")
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(), null, receiverId, "제목", "내용", NotificationLevel.INFO
    );

    given(notificationQDSLRepository.findAllWithCursor(request, receiverId)).willReturn(List.of(notification));
    given(notificationQDSLRepository.countWithCondition(receiverId)).willReturn(1L);
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    // when
    CursorResponse<NotificationDto> result = notificationService.findAll(request, receiverId);

    // then
    assertThat(result.data()).hasSize(1);
    assertThat(result.totalCount()).isEqualTo(1L);
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  @DisplayName("알림 목록이 limit개를 초과하면 hasNext가 true를 반환한다.")
  void findAll_success_with_has_next() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    NotificationSearchRequest request = new NotificationSearchRequest(
        null, null, 1, SortDirection.DESCENDING, NotificationSortBy.createdAt
    );

    Notification notification1 = Notification.builder()
        .receiverId(receiverId).type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title("제목1").content("내용1").level(NotificationLevel.INFO).build();
    Notification notification2 = Notification.builder()
        .receiverId(receiverId).type(NotificationType.CONTENT_ADDED)
        .title("제목2").content("내용2").level(NotificationLevel.INFO).build();

    ReflectionTestUtils.setField(notification1, "id", notificationId);
    ReflectionTestUtils.setField(notification1, "createdAt", java.time.Instant.now());

    NotificationDto dto = new NotificationDto(
        notificationId, null, receiverId, "제목1", "내용1", NotificationLevel.INFO
    );

    given(notificationQDSLRepository.findAllWithCursor(request, receiverId))
        .willReturn(new ArrayList<>(List.of(notification1, notification2)));
    given(notificationQDSLRepository.countWithCondition(receiverId)).willReturn(2L);
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    // when
    CursorResponse<NotificationDto> result = notificationService.findAll(request, receiverId);

    // then
    assertThat(result.data()).hasSize(1);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("알림을 읽음 처리하면 isRead가 true가 된다.")
  void markAsRead_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title("제목")
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
        .willReturn(Optional.of(notification));

    // when
    notificationService.markAsRead(notificationId, receiverId);

    // then
    assertThat(notification.isRead()).isTrue();
  }

  @Test
  @DisplayName("존재하지 않는 알림을 읽음 처리하면 예외가 발생한다.")
  void markAsRead_fail_with_not_found() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> notificationService.markAsRead(notificationId, receiverId))
        .isInstanceOf(NotificationException.class)
        .satisfies(e -> assertThat(((NotificationException) e).getErrorCode())
            .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
  }

  @Test
  @DisplayName("본인 알림이 아닌 알림을 읽음 처리하면 404를 반환한다.")
  void markAsRead_fail_with_forbidden() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    given(notificationRepository.findByIdAndReceiverId(notificationId, receiverId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> notificationService.markAsRead(notificationId, receiverId))
        .isInstanceOf(NotificationException.class)
        .satisfies(e -> assertThat(((NotificationException) e).getErrorCode())
            .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
  }

  @Test
  @DisplayName("권한 변경 알림을 저장하면 이전 권한과 새 권한이 내용에 포함된다.")
  void sendRoleUpdatedNotification_success() {
    UUID receiverId = UUID.randomUUID();
    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(),
        null,
        receiverId,
        NotificationType.ROLE_CHANGED.getTitle(),
        "내 권한이 [USER]에서 [ADMIN]로 변경되었어요",
        NotificationLevel.INFO
    );
    given(notificationRepository.save(any(Notification.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    NotificationDto result =
        notificationService.sendRoleUpdatedNotification(receiverId, Role.USER, Role.ADMIN);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    Notification saved = captor.getValue();
    assertThat(saved.getReceiverId()).isEqualTo(receiverId);
    assertThat(saved.getType()).isEqualTo(NotificationType.ROLE_CHANGED);
    assertThat(saved.getContent()).isEqualTo("내 권한이 [USER]에서 [ADMIN]로 변경되었어요");
    assertThat(result).isEqualTo(dto);
  }

  @Test
  @DisplayName("플레이리스트 구독 알림을 저장하면 NotificationDto를 반환한다.")
  void sendPlaylistSubscribedNotification_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title(NotificationType.PLAYLIST_SUBSCRIBED.getTitle())
        .content("테스트님이 '테스트 플레이리스트' 플레이리스트를 구독했어요.")
        .level(NotificationLevel.INFO)
        .build();

    NotificationDto dto = new NotificationDto(UUID.randomUUID(), null, receiverId,
        NotificationType.PLAYLIST_SUBSCRIBED.getTitle(), "내용", NotificationLevel.INFO);

    given(notificationRepository.save(any(Notification.class))).willReturn(notification);
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    // when
    NotificationDto result = notificationService.sendPlaylistSubscribedNotification(
        receiverId, "테스트", "테스트 플레이리스트");

    // then
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    assertThat(captor.getValue().getContent())
        .isEqualTo("테스트님이 '테스트 플레이리스트' 플레이리스트를 구독했어요.");
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("콘텐츠 추가 알림을 저장하면 NotificationDto 목록을 반환한다.")
  void sendContentAddedNotification_success() {
    // given
    UUID receiverId1 = UUID.randomUUID();
    UUID receiverId2 = UUID.randomUUID();
    List<UUID> receiverIds = List.of(receiverId1, receiverId2);

    Notification notification1 = Notification.builder()
        .receiverId(receiverId1)
        .type(NotificationType.CONTENT_ADDED)
        .title(NotificationType.CONTENT_ADDED.getTitle())
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    Notification notification2 = Notification.builder()
        .receiverId(receiverId2)
        .type(NotificationType.CONTENT_ADDED)
        .title(NotificationType.CONTENT_ADDED.getTitle())
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    NotificationDto dto = new NotificationDto(UUID.randomUUID(), null, receiverId1,
        NotificationType.CONTENT_ADDED.getTitle(), "내용", NotificationLevel.INFO);

    given(notificationRepository.saveAll(any())).willReturn(List.of(notification1, notification2));
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    // when
    List<NotificationDto> result = notificationService.sendContentAddedNotification(
        receiverIds, "테스트 플레이리스트", "테스트 콘텐츠");

    // then
    assertThat(result).hasSize(2);
    verify(notificationRepository).saveAll(any());
  }

  @Test
  @DisplayName("팔로우 알림을 저장하면 NotificationDto를 반환한다.")
  void sendFollowedNotification_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.FOLLOWED)
        .title(NotificationType.FOLLOWED.getTitle())
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    NotificationDto dto = new NotificationDto(UUID.randomUUID(), null, receiverId,
        NotificationType.FOLLOWED.getTitle(), "내용", NotificationLevel.INFO);

    given(notificationRepository.save(any(Notification.class))).willReturn(notification);
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    // when
    NotificationDto result = notificationService.sendFollowedNotification(receiverId, "테스트");

    // then
    assertThat(result).isNotNull();
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  @DisplayName("DM 수신 알림을 저장하면 NotificationDto를 반환한다.")
  void sendDmReceivedNotification_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.DM_RECEIVED)
        .title("DM 수신 알림")
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    NotificationDto dto = new NotificationDto(UUID.randomUUID(), null, receiverId,
        "DM 수신 알림", "내용", NotificationLevel.INFO);

    given(notificationRepository.save(any(Notification.class))).willReturn(notification);
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    // when
    NotificationDto result = notificationService.sendDmReceivedNotification(
        receiverId, "테스트", "안녕하세요");

    // then
    assertThat(result).isNotNull();
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  @DisplayName("팔로우 활동 알림을 저장하면 NotificationDto 목록을 반환한다.")
  void sendFollowActivityNotification_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    List<UUID> receiverIds = List.of(receiverId);

    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.FOLLOW_ACTIVITY)
        .title(NotificationType.FOLLOW_ACTIVITY.getTitle())
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    NotificationDto dto = new NotificationDto(UUID.randomUUID(), null, receiverId,
        NotificationType.FOLLOW_ACTIVITY.getTitle(), "내용", NotificationLevel.INFO);

    given(notificationRepository.saveAll(any())).willReturn(List.of(notification));
    given(notificationMapper.toDto(any(Notification.class))).willReturn(dto);

    // when
    List<NotificationDto> result = notificationService.sendFollowActivityNotification(
        receiverIds, "테스트", "새 리뷰를 작성했습니다.");

    // then
    assertThat(result).hasSize(1);
    verify(notificationRepository).saveAll(any());
  }
}
