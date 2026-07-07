package com.team6.moduply.notification.mapper;

import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

  public NotificationDto toDto(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getCreatedAt(),
        notification.getReceiverId(),
        notification.getTitle(),
        notification.getContent(),
        notification.getLevel()
    );
  }

}
