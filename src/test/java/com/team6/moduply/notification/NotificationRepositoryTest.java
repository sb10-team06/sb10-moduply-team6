package com.team6.moduply.notification;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.notification.dto.NotificationSearchRequest;
import com.team6.moduply.notification.dto.NotificationSortBy;
import com.team6.moduply.notification.entity.Notification;
import com.team6.moduply.notification.enums.NotificationLevel;
import com.team6.moduply.notification.enums.NotificationType;
import com.team6.moduply.notification.repository.NotificationRepository;
import com.team6.moduply.notification.repository.qdsl.NotificationQDSLRepository;
import com.team6.moduply.notification.repository.qdsl.NotificationQDSLRepositoryImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaAuditingConfig.class, NotificationQDSLRepositoryImpl.class})
class NotificationRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private NotificationQDSLRepository notificationQDSLRepository;

  private Notification saveNotification(UUID receiverId) {
    return notificationRepository.save(Notification.builder()
        .receiverId(receiverId)
        .type(NotificationType.PLAYLIST_SUBSCRIBED)
        .title("제목")
        .content("내용")
        .level(NotificationLevel.INFO)
        .build());
  }

  @Test
  @DisplayName("수신자 ID로 읽지 않은 알림 목록을 조회한다.")
  void findAllWithCursor_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    saveNotification(receiverId);
    saveNotification(receiverId);
    saveNotification(UUID.randomUUID()); // 다른 사용자 알림

    NotificationSearchRequest request = new NotificationSearchRequest(
        null, null, 10, SortDirection.DESCENDING, NotificationSortBy.createdAt
    );

    // when
    List<Notification> result = notificationQDSLRepository.findAllWithCursor(request, receiverId);

    // then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(n -> n.getReceiverId().equals(receiverId));
  }

  @Test
  @DisplayName("읽은 알림은 목록 조회에서 제외된다.")
  void findAllWithCursor_excludes_read_notifications() {
    // given
    UUID receiverId = UUID.randomUUID();
    Notification unread = saveNotification(receiverId);
    Notification read = saveNotification(receiverId);
    read.markAsRead();
    notificationRepository.save(read);

    NotificationSearchRequest request = new NotificationSearchRequest(
        null, null, 10, SortDirection.DESCENDING, NotificationSortBy.createdAt
    );

    // when
    List<Notification> result = notificationQDSLRepository.findAllWithCursor(request, receiverId);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(unread.getId());
  }

  @Test
  @DisplayName("읽지 않은 알림 개수를 반환한다.")
  void countWithCondition_success() {
    // given
    UUID receiverId = UUID.randomUUID();
    saveNotification(receiverId);
    saveNotification(receiverId);
    Notification read = saveNotification(receiverId);
    read.markAsRead();
    notificationRepository.save(read);

    // when
    long count = notificationQDSLRepository.countWithCondition(receiverId);

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("limit보다 많은 알림이 있으면 limit+1개를 반환한다.")
  void findAllWithCursor_success_with_has_next() {
    // given
    UUID receiverId = UUID.randomUUID();
    saveNotification(receiverId);
    saveNotification(receiverId);
    saveNotification(receiverId);

    NotificationSearchRequest request = new NotificationSearchRequest(
        null, null, 2, SortDirection.DESCENDING, NotificationSortBy.createdAt
    );

    // when
    List<Notification> result = notificationQDSLRepository.findAllWithCursor(request, receiverId);

    // then
    assertThat(result).hasSize(3); // limit+1
  }
}
