package com.team6.moduply.notification.repository.qdsl;

import com.team6.moduply.notification.dto.NotificationSearchRequest;
import com.team6.moduply.notification.entity.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationQDSLRepository {

  List<Notification> findAllWithCursor(NotificationSearchRequest request, UUID receiverId);

  long countWithCondition(UUID receiverId);

}
