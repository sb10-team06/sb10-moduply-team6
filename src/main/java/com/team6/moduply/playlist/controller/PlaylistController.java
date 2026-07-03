package com.team6.moduply.playlist.controller;

import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
      @RequestBody @Valid PlaylistCreateRequest request,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    UUID ownerId = userDetails.getUserDto().getId();
    return ResponseEntity.status(201).body(playlistService.create(request, ownerId));
  }

  @PatchMapping("/{playlistId}")
  @Operation(summary = "플레이리스트 수정", description = "플레이리스트 제목, 설명을 수정합니다.")
  public ResponseEntity<PlaylistDto> updatePlaylist(
      @PathVariable UUID playlistId,
      @RequestBody @Valid PlaylistUpdateRequest request,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    UUID ownerId = userDetails.getUserDto().getId();
    return ResponseEntity.ok(playlistService.update(playlistId, request, ownerId));
  }

  @DeleteMapping("/{playlistId}")
  @Operation(summary = "플레이리스트 삭제", description = "플레이리스트를 삭제합니다.")
  public ResponseEntity<Void> deletePlaylist(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    UUID ownerId = userDetails.getUserDto().getId();
    playlistService.delete(playlistId, ownerId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{playlistId}")
  @Operation(summary = "플레이리스트 단건 조회", description = "플레이리스트 상세 정보를 조회합니다.")
  public ResponseEntity<PlaylistDto> getPlaylist(
      @PathVariable UUID playlistId) {
    return ResponseEntity.ok(playlistService.findById(playlistId));
  }

  @GetMapping
  @Operation(summary = "플레이리스트 목록 조회", description = "플레이리스트 목록을 커서 기반 페이지네이션으로 조회합니다.")
  public ResponseEntity<CursorResponse<PlaylistDto>> getPlaylists(
      @ModelAttribute @Valid PlaylistSearchRequest request) {
    return ResponseEntity.ok(playlistService.findAll(request));
  }

  @PostMapping("/{playlistId}/contents/{contentId}")
  @Operation(summary = "플레이리스트 콘텐츠 추가", description = "플레이리스트에 콘텐츠를 추가합니다.")
  public ResponseEntity<Void> addContent(
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
  UUID ownerId = userDetails.getUserDto().getId();
  playlistService.addContent(playlistId, contentId, ownerId);
  return ResponseEntity.status(201).build();
  }

  @DeleteMapping("/{playlistId}/contents/{contentId}")
  @Operation(summary = "플레이리스트 콘텐츠 삭제", description = "플레이리스트에서 콘텐츠를 삭제합니다.")
  public ResponseEntity<Void> removeContent(
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails){
    UUID ownerId = userDetails.getUserDto().getId();
    playlistService.removeContent(playlistId, contentId, ownerId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/subscriptions")
  @Operation(summary = "플레이리스트 구독", description = "플레이리스트를 구독합니다.")
  public ResponseEntity<Void> subscribe(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    UUID subscriberId = userDetails.getUserDto().getId();
    playlistService.subscribe(playlistId, subscriberId);
    return ResponseEntity.status(201).build();
  }

  @DeleteMapping("/{playlistId}/subscriptions")
  @Operation(summary = "플레이리스트 구독취소", description = "플레이리스트 구독을 취소합니다.")
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal ModuPlyUserDetails userDetails) {
    UUID subscriberId = userDetails.getUserDto().getId();
    playlistService.unsubscribe(playlistId, subscriberId);
    return ResponseEntity.noContent().build();
  }

}
