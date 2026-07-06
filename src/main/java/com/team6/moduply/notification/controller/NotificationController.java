package com.team6.moduply.notification.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.notification.dto.NotificationDto;
import com.team6.moduply.notification.dto.NotificationSearchRequest;
import com.team6.moduply.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관련 API")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @Operation(summary = "알림 목록 조회", description = "API 요청자의 알림 목록을 커서 기반 페이지네이션으로 조회합니다.")
  public ResponseEntity<CursorResponse<NotificationDto>> getNotifications(
      @ModelAttribute @Valid NotificationSearchRequest request,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    UUID receiverId = userDetails.getUserDto().getId();
    return ResponseEntity.ok(notificationService.findAll(request, receiverId));
  }
}
