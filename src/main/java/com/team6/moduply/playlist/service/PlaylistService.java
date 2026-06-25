package com.team6.moduply.playlist.service;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.exception.PlaylistErrorCode;
import com.team6.moduply.playlist.exception.PlaylistException;
import com.team6.moduply.playlist.mapper.PlaylistMapper;
import com.team6.moduply.playlist.repository.PlaylistRepository;
import com.team6.moduply.playlist.repository.qdsl.PlaylistQDSLRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistMapper playlistMapper;
  private final PlaylistQDSLRepository playlistQDSLRepository;

  @Transactional
  public PlaylistDto create(PlaylistCreateRequest request, UUID ownerId) {
    // TODO: ownerId는 현재 임시로 파라미터로 받음
    // TODO: 인증 담당자 작업 완료 후 @AuthenticationPrincipal 또는 SecurityContextHolder로 교체 필요
    Playlist playlist = Playlist.builder()
        .ownerId(ownerId)
        .title(request.title())
        .description(request.description())
        .build();

    Playlist saved = playlistRepository.save(playlist);

    return playlistMapper.toDto(saved);
  }

  @Transactional
  public PlaylistDto update(UUID playlistId, PlaylistUpdateRequest request, UUID ownerId) {
    // TODO: 인증 담당자 작업 완료 후 ownerId 교체 필요
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND, playlistId));
// TODO: 인증 연동 후 소유자 검증 활성화 필요
//    if (!playlist.getOwnerId().equals(ownerId)) {
//      throw new PlaylistException(PlaylistErrorCode.PLAYLIST_FORBIDDEN, playlistId);
//    }

    playlist.update(request.title(), request.description());

    return playlistMapper.toDto(playlist);
  }

  @Transactional
  public void delete(UUID playlistId, UUID ownerId) {
    // TODO: 인증 담당자 작업 완료 후 ownerId 교체 필요
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND, playlistId));

// TODO: 인증 연동 후 소유자 검증 활성화 필요
//    if (!playlist.getOwnerId().equals(ownerId)) {
//      throw new PlaylistException(PlaylistErrorCode.PLAYLIST_FORBIDDEN, playlistId);
//    }

    playlistRepository.delete(playlist);
  }

  @Transactional(readOnly = true)
  public PlaylistDto findById(UUID playlistId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND, playlistId));

    return playlistMapper.toDto(playlist);
  }

  @Transactional(readOnly = true)
  public CursorResponse<PlaylistDto> findAll(PlaylistSearchRequest request) {
    // limit + 1개 조회 (sentinel 방식)
    List<Playlist> playlists = playlistQDSLRepository.findAllWithCursor(request);
    long totalCount = playlistQDSLRepository.countWithCondition(request);

    boolean hasNext = playlists.size() > request.limit();

    // hasNext면 sentinel 레코드 제거
    if (hasNext) {
      playlists = playlists.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      Playlist last = playlists.get(playlists.size() - 1);
      nextCursor = last.getUpdatedAt().toString();
      nextIdAfter = last.getId();
    }

    List<PlaylistDto> data = playlists.stream()
        .map(playlistMapper::toDto)
        .toList();

    return new CursorResponse<>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection()
    );
  }
}
