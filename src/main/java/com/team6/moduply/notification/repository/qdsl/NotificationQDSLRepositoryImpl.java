package com.team6.moduply.notification.repository.qdsl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.notification.dto.NotificationSearchRequest;
import com.team6.moduply.notification.entity.Notification;
import com.team6.moduply.notification.entity.QNotification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQDSLRepositoryImpl implements NotificationQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private final QNotification notification = QNotification.notification;

  @Override
  public List<Notification> findAllWithCursor(NotificationSearchRequest request, UUID receiverId) {
    return queryFactory.selectFrom(notification)
        .where(
            notification.receiverId.eq(receiverId),
            notification.isRead.eq(false),
            cursorCondition(request)
        )
        .orderBy(
            request.sortDirection() == SortDirection.ASCENDING
                ? notification.createdAt.asc()
                : notification.createdAt.desc(),
            notification.id.asc()
        )
        .limit((long) request.limit() + 1)
        .fetch();
  }

  @Override
  public long countWithCondition(UUID receiverId) {
    Long result = queryFactory.select(notification.count())
        .from(notification)
        .where(
            notification.receiverId.eq(receiverId),
            notification.isRead.eq(false))
        .fetchOne();
    return result != null ? result : 0L;
  }

  private BooleanExpression cursorCondition(NotificationSearchRequest request) {
    if (request.cursor() == null) {
      return null;
    }

    Instant cursorTime = Instant.parse(request.cursor());
    if (request.sortDirection() == SortDirection.ASCENDING) {
      return notification.createdAt.gt(cursorTime)
          .or(notification.createdAt.eq(cursorTime).and(notification.id.gt(request.idAfter())));
    } else {
      return notification.createdAt.lt(cursorTime)
          .or(notification.createdAt.eq(cursorTime).and(notification.id.gt(request.idAfter())));
    }
  }
}
