package com.team6.moduply.watching.controller;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.watching.controller.api.WatchingSessionApi;
import com.team6.moduply.watching.dto.WatchingSessionDto;
import com.team6.moduply.watching.dto.WatchingSessionQueryCondition;
import com.team6.moduply.watching.service.WatchingSessionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WatchingSessionController implements WatchingSessionApi {

  private final WatchingSessionService watchingSessionService;

  @GetMapping("/users/{watcherId}/watching-sessions")
  public ResponseEntity<WatchingSessionDto> findWatchingSessionsByWatcher(
      @PathVariable UUID watcherId) {
    WatchingSessionDto response = watchingSessionService.findByUserId(watcherId);
    if (response == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(response);
  }

  @GetMapping("/contents/{contentId}/watching-sessions")
  public ResponseEntity<CursorResponse<WatchingSessionDto>> findWatchingSessionsByContent(
      @PathVariable UUID contentId,
      @ModelAttribute WatchingSessionQueryCondition condition) {
    CursorResponse<WatchingSessionDto> response = watchingSessionService.findAllByContentId(
        contentId, condition);
    return ResponseEntity.ok(response);
  }
}
