package com.team6.moduply.playlist.mapper;

import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.entity.Playlist;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        copyContents(contents)
    );
  }

  private List<PlaylistDto.ContentSummaryDto> copyContents(
      List<PlaylistDto.ContentSummaryDto> contents) {
    if (contents == null) {
      return new ArrayList<>();
    }

    return contents.stream()
        .map(content -> new PlaylistDto.ContentSummaryDto(
            content.id(),
            content.type(),
            content.title(),
            content.description(),
            content.thumbnailUrl(),
            content.tags() == null ? new ArrayList<>() : new ArrayList<>(content.tags()),
            content.averageRating(),
            content.reviewCount()
        ))
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }
}
