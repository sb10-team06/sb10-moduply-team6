package com.team6.moduply.playlist.mapper;

import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.entity.Playlist;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlaylistMapper {

  public PlaylistDto toDto(Playlist playlist, PlaylistDto.OwnerDto owner,
      long subscriberCount, boolean subscribedByMe,
      List<PlaylistDto.ContentSummaryDto> contents) {
    return new PlaylistDto(
        playlist.getId(),
        owner,
        playlist.getTitle(),
        playlist.getDescription(),
        playlist.getUpdatedAt(),
        subscriberCount,
        subscribedByMe,
        contents
    );
  }
}
