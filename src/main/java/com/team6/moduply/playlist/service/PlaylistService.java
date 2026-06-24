package com.team6.moduply.playlist.service;

import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.mapper.PlaylistMapper;
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
  private final PlaylistMapper playlistMapper;

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
}
