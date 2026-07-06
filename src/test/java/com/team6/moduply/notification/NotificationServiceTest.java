package com.team6.moduply.notification;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.dto.NotificationSearchRequest;
import com.team6.moduply.notification.dto.NotificationSortBy;
import com.team6.moduply.notification.entity.Notification;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.enums.NotificationType;
import com.team6.moduply.notification.mapper.NotificationMapper;
import com.team6.moduply.notification.repository.NotificationRepository;
import com.team6.moduply.notification.repository.qdsl.NotificationQDSLRepository;
import com.team6.moduply.notification.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
    assertThat(result.nextIdAfter()).isEqualTo(notificationId);
  }
}
