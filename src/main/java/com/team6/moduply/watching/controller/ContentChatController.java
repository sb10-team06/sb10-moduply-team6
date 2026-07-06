package com.team6.moduply.watching.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.watching.dto.ContentChatSendRequest;
import com.team6.moduply.watching.service.ContentChatService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ContentChatController {

  private final ContentChatService contentChatService;
  private final Validator validator;

  @MessageMapping("/contents/{contentId}/chat")
  public void chat(ContentChatSendRequest request, @DestinationVariable UUID contentId,
      Authentication authentication) {
    Set<ConstraintViolation<ContentChatSendRequest>> violations = validator.validate(request);

    if (!violations.isEmpty()) {
      String errorMessage = violations.iterator().next().getMessage();
      log.warn("ContentChatSendRequest 입력값 인증 실패: errorMessage={}", errorMessage);
      throw new IllegalArgumentException("유효하지 않은 입력값입니다: " + errorMessage);
    }
    ModuPlyUserDetails userDetails = (ModuPlyUserDetails) authentication.getPrincipal();
    UUID userId = userDetails.getUserDto().getId();
    contentChatService.sendChatMessage(request, contentId, userId);
    log.info("실시간 채팅 전송: userId={}, contentId={}", userId, contentId);
  }
}
