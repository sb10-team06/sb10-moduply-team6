package com.team6.moduply.playlist.controller;

import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@Tag(name = "Playlist", description = "플레이리스트 관련 API")
public class PlaylistController {

  private final PlaylistService playlistService;

  @PostMapping
  @Operation(summary = "플레이리스트 생성", description = "새로운 플레이리스트를 생성합니다.")
  public ResponseEntity<PlaylistDto> createPlaylist(
      @RequestBody @Valid PlaylistCreateRequest request) {
    // TODO: 인증 담당자 작업 완료 후 ownerId 교체 필요
    UUID tempOwnerId = UUID.randomUUID();
    return ResponseEntity.status(201).body(playlistService.create(request, tempOwnerId));
  }

  @PatchMapping("/{playlistId}")
  @Operation(summary = "플레이리스트 수정", description = "플레이리스트 제목, 설명을 수정합니다.")
  public ResponseEntity<PlaylistDto> updatePlaylist(
      @PathVariable UUID playlistId,
      @RequestBody @Valid PlaylistUpdateRequest request) {
    // TODO: 인증 담당자 작업 완료 후 @AuthenticationPrincipal로 ownerId 교체 필요
    UUID tempOwnerId = UUID.randomUUID();
    return ResponseEntity.ok(playlistService.update(playlistId, request, tempOwnerId));
  }

  @DeleteMapping("/{playlistId}")
  @Operation(summary = "플레이리스트 삭제", description = "플레이리스트를 삭제합니다.")
  public ResponseEntity<Void> deletePlaylist(
      @PathVariable UUID playlistId) {
    // TODO: 인증 담당자 작업 완료 후 @AuthenticationPrincipal로 ownerId 교체 필요
    UUID tempOwnerId = UUID.randomUUID();
    playlistService.delete(playlistId, tempOwnerId);
    return ResponseEntity.noContent().build();
  }
}
