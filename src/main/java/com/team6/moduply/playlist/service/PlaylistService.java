package com.team6.moduply.playlist.service;

import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.repository.PlaylistRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;

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

    return toDto(saved);
  }

  private PlaylistDto toDto(Playlist playlist) {
    // TODO: owner 정보는 User 도메인 담당자 작업 완료 후 실제 User 조회로 교체 필요
    // TODO: contents, subscriberCount, subscribedByMe는 연관 테이블 조회 후 채워야 함
    return new PlaylistDto(
        playlist.getId(),
        null,               // owner: User 도메인 연동 후 채우기
        playlist.getTitle(),
        playlist.getDescription(),
        playlist.getUpdatedAt(),
        0L,         // subscriberCount: 조회 로직 추가 후 채우기
        false,                    // subscribedByMe: 인증 연동 후 채우기
        List.of()                 // contents: PlaylistContent 조회 로직 추가 후 채우기
    );
  }
}
