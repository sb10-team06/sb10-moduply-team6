package com.team6.moduply.playlist.mapper;

import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.entity.Playlist;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlaylistMapper {

  public PlaylistDto toDto(Playlist playlist) {
    // TODO: owner 정보는 User 도메인 담당자 작업 완료 후 실제 User 조회로 교체 필요
    // TODO: contents, subscriberCount, subscribedByMe는 연관 테이블 조회 후 채워야 함
    return new PlaylistDto(
        playlist.getId(),
        null,       // owner: User 도메인 연동 후 채우기
        playlist.getTitle(),
        playlist.getDescription(),
        playlist.getUpdatedAt(),
        0L,         // subscriberCount: 조회 로직 추가 후 채우기
        false,      // subscribedByMe: 인증 연동 후 채우기
        List.of()   // contents: PlaylistContent 조회 로직 추가 후 채우기
    );
  }
}
