package com.team6.moduply.watching.service;

import com.team6.moduply.user.dto.UserSummary;
import com.team6.moduply.watching.dto.ContentChatDto;
import com.team6.moduply.watching.dto.ContentChatSendRequest;
import com.team6.moduply.watching.exception.WatchingSessionErrorCode;
import com.team6.moduply.watching.exception.WatchingSessionException;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentChatService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final SimpMessagingTemplate simpMessagingTemplate;

  public void sendChatMessage(ContentChatSendRequest request,
      UUID contentId, UUID userId) {
    WatchingSession watchingSession = watchingSessionRepository.findByUserId(userId)
        .orElseThrow(
            () -> {
              log.error("해당 사용자의 시청 세션을 찾을 수 없음. userId={}", userId);
              return new WatchingSessionException(
                  WatchingSessionErrorCode.WATCHING_SESSION__NOT_FOUND,
                  Map.of("userId", userId));
            });
    if (!contentId.equals(watchingSession.getContentId())) {
      log.error("구독한 시청세션과 채팅방의 콘텐츠 아이디 불일치 userId={}, watching-contentId={}, chat-contentId={}",
          userId, watchingSession.getContentId(), contentId);
      throw new WatchingSessionException(
          WatchingSessionErrorCode.WATCHING_SESSION__CONTENT_MISMATCH,
          Map.of(
              "userId", userId,
              "watching-session-contentId", watchingSession.getContentId(),
              "content-chat-contentId", contentId));
    }
    String destination = String.format("/sub/contents/%s/chat", contentId);
    UserSummary sender = watchingSession.getWatcher();
    // TODO: [김민형] 욕설 필터링 추가
    ContentChatDto message = new ContentChatDto(sender, request.content());
    simpMessagingTemplate.convertAndSend(destination, message);
  }
}
