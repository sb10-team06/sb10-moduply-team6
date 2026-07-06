package com.team6.moduply.playlist.service;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.notification.event.ContentAddedEvent;
import com.team6.moduply.notification.event.FollowActivityEvent;
import com.team6.moduply.notification.event.PlaylistSubscribedEvent;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.entity.PlaylistContent;
import com.team6.moduply.playlist.entity.PlaylistSubscription;
import com.team6.moduply.playlist.exception.PlaylistErrorCode;
import com.team6.moduply.playlist.exception.PlaylistException;
import com.team6.moduply.playlist.mapper.PlaylistMapper;
import com.team6.moduply.playlist.repository.PlaylistContentRepository;
import com.team6.moduply.playlist.repository.PlaylistRepository;
import com.team6.moduply.playlist.repository.PlaylistSubscriptionRepository;
import com.team6.moduply.playlist.repository.qdsl.PlaylistQDSLRepository;
import java.util.List;
import java.util.Map;

import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final UserRepository userRepository;

  @Transactional
  public PlaylistDto create(PlaylistCreateRequest request, UUID ownerId) {

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId)
        .title(request.title())
        .description(request.description())
        .build();

    Playlist saved = playlistRepository.save(playlist);
    String ownerName = userRepository.findById(ownerId)
                    .map(User::getName)
                            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of("userId",  ownerId)));
    eventPublisher.publishEvent(new FollowActivityEvent(
        ownerId,
        ownerName,
        "새 플레이리스트를 생성했습니다."
    ));

    return playlistMapper.toDto(saved);
  }

  @Transactional
  public PlaylistDto update(UUID playlistId, PlaylistUpdateRequest request, UUID ownerId) {

    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    validateOwner(playlist, ownerId, playlistId);

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

    validateOwner(playlist, ownerId, playlistId);

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

    validateOwner(playlist, ownerId, playlistId);

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
    String ownerName = userRepository.findById(ownerId)
            .map(User::getName)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND_EXCEPTION, Map.of("userId",  ownerId)));

    eventPublisher.publishEvent(new ContentAddedEvent(
        playlistId,
        playlist.getTitle(),
        String.valueOf(contentId)
    ));
    eventPublisher.publishEvent(new FollowActivityEvent(
            ownerId,
            ownerName,
            "새 플레이리스트를 생성했습니다."
    ));
  }

  @Transactional
  public void removeContent(UUID playlistId, UUID contentId, UUID ownerId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    validateOwner(playlist, ownerId, playlistId);

    PlaylistContent playlistContent = playlistContentRepository
        .findByPlaylistAndContentId(playlist, contentId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_CONTENT_NOT_FOUND,
            Map.of("playlistId", playlistId, "contentId", contentId)
        ));

    playlistContentRepository.delete(playlistContent);
  }

  private void validateOwner(Playlist playlist, UUID ownerId, UUID playlistId) {
    if (!playlist.getOwnerId().equals(ownerId)) {
      throw new PlaylistException(
          PlaylistErrorCode.PLAYLIST_FORBIDDEN,
          Map.of("playlistId", playlistId)
      );
    }
  }

  @Transactional
  public void subscribe(UUID playlistId, UUID subscriberId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    if (playlist.getOwnerId().equals(subscriberId)) {
      throw new PlaylistException(
          PlaylistErrorCode.PLAYLIST_SELF_SUBSCRIPTION,
          Map.of("playlistId", playlistId)
      );
    }

    if (playlistSubscriptionRepository.existsByPlaylistAndSubscriberId(playlist, subscriberId)) {
      throw new PlaylistException(
          PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS,
          Map.of("playlistId", playlistId)
      );
    }

    PlaylistSubscription subscription = PlaylistSubscription.builder()
        .playlist(playlist)
        .subscriberId(subscriberId)
        .build();

    try {
      playlistSubscriptionRepository.save(subscription);
    } catch (DataIntegrityViolationException e) {
      throw new PlaylistException(
          PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS,
          Map.of("playlistId", playlistId),
          e
      );
    }
    // 이벤트 발행
    eventPublisher.publishEvent(new PlaylistSubscribedEvent(
        playlist.getOwnerId(),
        subscriberId,
        playlistId,
        playlist.getTitle()
    ));
  }

  @Transactional
  public void unsubscribe(UUID playlistId, UUID subscriberId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    PlaylistSubscription subscription = playlistSubscriptionRepository
        .findByPlaylistAndSubscriberId(playlist, subscriberId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    playlistSubscriptionRepository.delete(subscription);
  }
}
