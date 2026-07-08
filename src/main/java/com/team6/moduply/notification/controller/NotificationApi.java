package com.team6.moduply.notification.controller;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.dto.NotificationSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;

@Tag(name = "Notification", description = "알림 관련 API")
public interface NotificationApi {

  @Operation(summary = "알림 목록 조회", description = "API 요청자의 알림 목록을 커서 기반 페이지네이션으로 조회합니다.")
  ResponseEntity<CursorResponse<NotificationDto>> getNotifications(
      @ModelAttribute @Valid NotificationSearchRequest request,
      UUID receiverId
  );

  @Operation(summary = "알림 읽음 처리", description = "알림을 읽음 처리합니다.")
  ResponseEntity<Void> markAsRead(UUID notificationId, UUID receiverId);
}
