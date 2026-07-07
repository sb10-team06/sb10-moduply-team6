package com.team6.moduply.sse;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "SSE", description = "SSE 관련 API")
public class SseController {

  private final SseEmitterManager sseEmitterManager;

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "SSE 연결", description = "실시간 알림을 위한 SSE 연결을 수립합니다.")
  public SseEmitter subscribe(
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }
    UUID userId = userDetails.getUserDto().getId();
    return sseEmitterManager.connect(userId);
  }
}
