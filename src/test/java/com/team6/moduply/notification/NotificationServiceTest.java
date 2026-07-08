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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
    assertThat(result.nextIdAfter()).isEqualTo(notificationId);
  }

  @Test
  @DisplayName("hasNext가 true인데 마지막 알림의 createdAt이 null이면 예외가 발생한다.")
  void findAll_fail_with_invalid_state() {
    // given
    UUID receiverId = UUID.randomUUID();
    NotificationSearchRequest request = new NotificationSearchRequest(
        null, null, 1, SortDirection.DESCENDING, NotificationSortBy.createdAt
    );

    Notification notification1 = Notification.builder()
        .receiverId(receiverId).type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title("제목1").content("내용1").level(NotificationLevel.INFO).build();
    Notification notification2 = Notification.builder()
        .receiverId(receiverId).type(NotificationType.CONTENT_ADDED)
        .title("제목2").content("내용2").level(NotificationLevel.INFO).build();

    // createdAt을 null로 유지 (주입 안 함)
    ReflectionTestUtils.setField(notification1, "id", UUID.randomUUID());

    given(notificationQDSLRepository.findAllWithCursor(request, receiverId))
        .willReturn(new ArrayList<>(List.of(notification1, notification2)));
    given(notificationQDSLRepository.countWithCondition(receiverId)).willReturn(2L);

    // when & then
    assertThatThrownBy(() -> notificationService.findAll(request, receiverId))
        .isInstanceOf(NotificationException.class)
        .satisfies(e -> assertThat(((NotificationException) e).getErrorCode())
            .isEqualTo(NotificationErrorCode.NOTIFICATION_INVALID_STATE));
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

    given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

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

    given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> notificationService.markAsRead(notificationId, receiverId))
        .isInstanceOf(NotificationException.class)
        .satisfies(e -> assertThat(((NotificationException) e).getErrorCode())
            .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
  }

  @Test
  @DisplayName("본인 알림이 아닌 알림을 읽음 처리하면 예외가 발생한다.")
  void markAsRead_fail_with_forbidden() {
    // given
    UUID receiverId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    Notification notification = Notification.builder()
        .receiverId(otherId)
        .type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title("제목")
        .content("내용")
        .level(NotificationLevel.INFO)
        .build();

    given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

    // when & then
    assertThatThrownBy(() -> notificationService.markAsRead(notificationId, receiverId))
        .isInstanceOf(NotificationException.class)
        .satisfies(e -> assertThat(((NotificationException) e).getErrorCode())
            .isEqualTo(NotificationErrorCode.NOTIFICATION_FORBIDDEN));
  }
}
