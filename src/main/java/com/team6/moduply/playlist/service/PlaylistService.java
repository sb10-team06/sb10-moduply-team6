package com.team6.moduply.playlist.service;

import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.content.entity.Content;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import com.team6.moduply.user.repository.UserRepository;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final BinaryContentService binaryContentService;

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

    return playlistMapper.toDto(saved, null, 0L, false, List.of());
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

    return playlistMapper.toDto(playlist, null, 0L, false, List.of());
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
  public PlaylistDto findById(UUID playlistId, UUID currentUserId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    PlaylistDto.OwnerDto ownerDto = userRepository.findById(playlist.getOwnerId())
        .map(user -> new PlaylistDto.OwnerDto(user.getId(), user.getName(), null))
        .orElse(null);

    // TODO: 실시간성이 중요하지 않으므로 캐싱 적용하기
    long subscriberCount = playlistSubscriptionRepository.countByPlaylist(playlist);
    boolean subscribedByMe = currentUserId != null &&
        playlistSubscriptionRepository.existsByPlaylistAndSubscriberId(playlist, currentUserId);

    List<PlaylistContent> playlistContents = playlistContentRepository.findAllByPlaylist(playlist);
    List<UUID> contentIds = playlistContents.stream()
        .map(PlaylistContent::getContentId)
        .toList();
    Map<UUID, Content> contentMap = contentRepository.findAllById(contentIds).stream()
        .collect(Collectors.toMap(Content::getId, c -> c));

    List<PlaylistDto.ContentSummaryDto> contents = playlistContents.stream()
        .map(pc -> {
          Content c = contentMap.get(pc.getContentId());
          if (c == null) return null;
          return new PlaylistDto.ContentSummaryDto(
              c.getId(),
              c.getType(),
              c.getTitle(),
              c.getDescription(),
              binaryContentService.generateUrl(c.getContentImg()),
              List.of(),
              c.getAverageRating() != null ? c.getAverageRating().doubleValue() : null,
              c.getReviewCount());
        })
        .filter(Objects::nonNull)
        .toList();

    return playlistMapper.toDto(playlist, ownerDto, subscriberCount, subscribedByMe, contents);
  }

  @Transactional(readOnly = true)
  public CursorResponse<PlaylistDto> findAll(PlaylistSearchRequest request, UUID currentUserId) {
    // 1. limit + 1개 조회 (sentinel 방식)
    List<Playlist> playlists = playlistQDSLRepository.findAllWithCursor(request);
    long totalCount = playlistQDSLRepository.countWithCondition(request);

    boolean hasNext = playlists.size() > request.limit();

    String nextCursor = null;
    UUID nextIdAfter = null;

    // 2. hasNext면 sentinel 레코드 제거 + 다음 커서 계산
    if (hasNext) {
      playlists = playlists.subList(0, request.limit());
      Playlist last = playlists.get(playlists.size() - 1);
      nextCursor = last.getUpdatedAt() != null ? last.getUpdatedAt().toString() : null;
      nextIdAfter = last.getId();
    }

    // 3. N+1 방지를 위한 일괄 조회
    // 필요한 ID 목록을 먼저 추출하고 한 번에 조회해 쿼리 수 최소화

    // 플레이리스트가 비어있는 경우 조기 반환
    if (playlists.isEmpty()) {
      return new CursorResponse<>(
          List.of(), null, null, false, totalCount,
          request.sortBy().name(), request.sortDirection()
      );
    }

    // 3-1. 소유자 일괄 조회 (profileImg JOIN 포함)
    List<UUID> ownerIds = playlists.stream().map(Playlist::getOwnerId).distinct().toList();
    Map<UUID, User> ownerMap = userRepository.findAllByIdWithProfileImg(ownerIds)
        .stream().collect(Collectors.toMap(User::getId, u -> u));

    // 3-2. 구독자 수 일괄 조회
    Map<UUID, Long> subscriberCountMap = new HashMap<>();
    playlistSubscriptionRepository.countByPlaylistIds(
        playlists.stream().map(Playlist::getId).toList()
    ).forEach(row -> subscriberCountMap.put((UUID) row[0], (Long) row[1]));

    // 3-3. 콘텐츠 일괄 조회 후 플레이리스트 ID 기준으로 그룹핑
    List<PlaylistContent> allContents = playlistContentRepository.findAllByPlaylistIn(playlists);
    List<UUID> contentIds = allContents.stream().map(PlaylistContent::getContentId).distinct().toList();
    Map<UUID, Content> contentMap = contentRepository.findAllById(contentIds)
        .stream().collect(Collectors.toMap(Content::getId, c -> c));
    Map<UUID, List<PlaylistContent>> contentsByPlaylist = allContents.stream()
        .collect(Collectors.groupingBy(pc -> pc.getPlaylist().getId()));

    // 3-4. 현재 사용자의 구독 플레이리스트 ID 일괄 조회
    Set<UUID> subscribedPlaylistIds = new HashSet<>();
    if (currentUserId != null) {
      List<UUID> playlistIds = playlists.stream().map(Playlist::getId).toList();
      subscribedPlaylistIds.addAll(
          playlistSubscriptionRepository.findSubscribedPlaylistIdsBySubscriberIdAndPlaylistIdIn(
              currentUserId, playlistIds)
      );
    }

    // 4. 조회한 데이터로 DTO 변환
    List<PlaylistDto> data = playlists.stream()
        .map(playlist -> {
          User owner = ownerMap.get(playlist.getOwnerId());
          PlaylistDto.OwnerDto ownerDto = owner != null
              ? new PlaylistDto.OwnerDto(owner.getId(), owner.getName(),
              binaryContentService.generateUrl(owner.getProfileImg()))
              : null;

          long subscriberCount = subscriberCountMap.getOrDefault(playlist.getId(), 0L);

          List<PlaylistDto.ContentSummaryDto> contents = contentsByPlaylist
              .getOrDefault(playlist.getId(), List.of())
              .stream()
              .map(pc -> {
                Content c = contentMap.get(pc.getContentId());
                if (c == null) return null;
                return new PlaylistDto.ContentSummaryDto(
                    c.getId(), c.getType(), c.getTitle(), c.getDescription(),
                    binaryContentService.generateUrl(c.getContentImg()),
                    List.of(),
                    c.getAverageRating() != null ? c.getAverageRating().doubleValue() : null,
                    c.getReviewCount());
              })
              .filter(Objects::nonNull)
              .toList();

          boolean subscribedByMe = subscribedPlaylistIds.contains(playlist.getId());
          return playlistMapper.toDto(playlist, ownerDto, subscriberCount, subscribedByMe, contents);
        })
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
            "플레이리스트에 새 콘텐츠를 추가했습니다."
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

    User subscriber = userRepository.findById(subscriberId)
        .orElseThrow(() -> new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("subscriberId", subscriberId)
        ));

    // 이벤트 발행
    eventPublisher.publishEvent(new PlaylistSubscribedEvent(
        playlist.getOwnerId(),
        subscriberId,
        playlistId,
        subscriber.getName(),
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
