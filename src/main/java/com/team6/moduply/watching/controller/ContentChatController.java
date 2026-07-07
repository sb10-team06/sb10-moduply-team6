package com.team6.moduply.watching.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.watching.dto.ContentChatSendRequest;
import com.team6.moduply.watching.service.ContentChatService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ContentChatController {

  private final ContentChatService contentChatService;

  @MessageMapping("/contents/{contentId}/chat")
  public void chat(@Valid ContentChatSendRequest request,
      @DestinationVariable UUID contentId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {

    UUID userId = userDetails.getUserDto().getId();
    contentChatService.sendChatMessage(request, contentId, userId);
    log.info("실시간 채팅 전송: userId={}, contentId={}", userId, contentId);
  }
}
