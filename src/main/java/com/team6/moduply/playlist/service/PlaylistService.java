package com.team6.moduply.playlist.service;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.entity.PlaylistContent;
import com.team6.moduply.playlist.exception.PlaylistErrorCode;
import com.team6.moduply.playlist.exception.PlaylistException;
import com.team6.moduply.playlist.mapper.PlaylistMapper;
import com.team6.moduply.playlist.repository.PlaylistContentRepository;
import com.team6.moduply.playlist.repository.PlaylistRepository;
import com.team6.moduply.playlist.repository.qdsl.PlaylistQDSLRepository;
import java.util.List;
import java.util.Map;
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
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;

  @Transactional
  public PlaylistDto create(PlaylistCreateRequest request, UUID ownerId) {

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

    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    if (!playlist.getOwnerId().equals(ownerId)) {
      throw new PlaylistException(PlaylistErrorCode.PLAYLIST_FORBIDDEN,
          Map.of("playlistId", playlistId));
    }

    playlist.update(request.title(), request.description());

    return playlistMapper.toDto(playlist);
  }

  @Transactional
  public void delete(UUID playlistId, UUID ownerId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    if (!playlist.getOwnerId().equals(ownerId)) {
      throw new PlaylistException(PlaylistErrorCode.PLAYLIST_FORBIDDEN,
          Map.of("playlistId", playlistId));
    }

    playlistRepository.delete(playlist);
  }

  @Transactional(readOnly = true)
  public PlaylistDto findById(UUID playlistId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

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
      nextCursor = last.getUpdatedAt() != null ? last.getUpdatedAt().toString() : null;
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

  @Transactional
  public void addContent(UUID playlistId, UUID contentId, UUID ownerId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

     if (!playlist.getOwnerId().equals(ownerId)) {
         throw new PlaylistException(PlaylistErrorCode.PLAYLIST_FORBIDDEN, Map.of("playlistId", playlistId));
     }

    if (!contentRepository.existsById(contentId)) {
      throw new PlaylistException(
          PlaylistErrorCode.CONTENT_NOT_FOUND,
          Map.of("contentId", contentId)
      );
    }

    boolean alreadyExists = playlistContentRepository
        .existsByPlaylistAndContentId(playlist, contentId);
    if (alreadyExists) {
      throw new PlaylistException(
          PlaylistErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS,
          Map.of("playlistId", playlistId, "contentId", contentId)
      );
    }

    PlaylistContent playlistContent = PlaylistContent.builder()
        .playlist(playlist)
        .contentId(contentId)
        .build();

    playlistContentRepository.save(playlistContent);
  }

  @Transactional
  public void removeContent(UUID playlistId, UUID contentId, UUID ownerId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

     if (!playlist.getOwnerId().equals(ownerId)) {
         throw new PlaylistException(PlaylistErrorCode.PLAYLIST_FORBIDDEN, Map.of("playlistId", playlistId));
     }

    PlaylistContent playlistContent = playlistContentRepository
        .findByPlaylistAndContentId(playlist, contentId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_CONTENT_NOT_FOUND,
            Map.of("playlistId", playlistId, "contentId", contentId)
        ));

    playlistContentRepository.delete(playlistContent);
  }
}
