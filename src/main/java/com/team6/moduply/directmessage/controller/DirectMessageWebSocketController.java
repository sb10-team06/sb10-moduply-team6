package com.team6.moduply.directmessage.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.directmessage.dto.DirectMessageCreateRequest;
import com.team6.moduply.directmessage.dto.DirectMessageDto;
import com.team6.moduply.directmessage.service.DirectMessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectMessageWebSocketController {
  // TODO: WebSocket 스레드풀 설정
  // TODO: DB 커넥션 풀 점검
  private static final String DIRECT_MESSAGE_DESTINATION =
      "/sub/conversations/%s/direct-messages";

  private final DirectMessageService directMessageService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/conversations/{conversationId}/direct-messages")
  public void create(
      @DestinationVariable UUID conversationId,
      @Valid @Payload DirectMessageCreateRequest request,
      Principal principal
  ) {
    UUID currentUserId = resolveCurrentUserId(principal);
    DirectMessageDto directMessage = directMessageService.create(
        conversationId,
        request,
        currentUserId
    );

    // TODO: event 비동기 처리, kafka 사용
    // TODO: DB 저장 -> 이벤트 발행 -> 별도 listener에서 broadcast
    // DM을 해당 대화방을 구독중인 클라이언트들에게 실시간으로 보냄
    messagingTemplate.convertAndSend(
        DIRECT_MESSAGE_DESTINATION.formatted(conversationId),
        directMessage
    );
  }

  private UUID resolveCurrentUserId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof ModuPlyUserDetails userDetails) {
      return userDetails.getUserDto().getId();
    }

    throw new AccessDeniedException("인증된 WebSocket 사용자 정보가 없습니다.");
  }
}
