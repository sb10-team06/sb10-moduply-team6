package com.team6.moduply.playlist;

import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.exception.PlaylistException;
import com.team6.moduply.playlist.mapper.PlaylistMapper;
import com.team6.moduply.playlist.repository.PlaylistRepository;
import com.team6.moduply.playlist.service.PlaylistService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

  @InjectMocks
  private PlaylistService playlistService;

  @Mock
  private PlaylistRepository playlistRepository;

  @Mock
  private PlaylistMapper playlistMapper;

  @Test
  @DisplayName("플레이리스트 생성 성공")
  void create_success() {
    // given
    UUID ownerId = UUID.randomUUID();
    PlaylistCreateRequest request = new PlaylistCreateRequest("내 최애 영화", "비 오는 날 보기 좋은 영화들");

    Playlist savedPlaylist = Playlist.builder()
        .ownerId(ownerId)
        .title(request.title())
        .description(request.description())
        .build();

    PlaylistDto expectedDto = new PlaylistDto(
        UUID.randomUUID(), null, "내 최애 영화",
        "비 오는 날 보기 좋은 영화들", null, 0L, false, List.of()
    );

    given(playlistRepository.save(any(Playlist.class))).willReturn(savedPlaylist);
    given(playlistMapper.toDto(savedPlaylist)).willReturn(expectedDto);

    // when
    PlaylistDto result = playlistService.create(request, ownerId);

    // then
    verify(playlistMapper).toDto(savedPlaylist);
    assertThat(result.title()).isEqualTo("내 최애 영화");
    assertThat(result.description()).isEqualTo("비 오는 날 보기 좋은 영화들");
  }

  @Test
  @DisplayName("제목이 없으면 플레이리스트 생성 실패")
  void create_fail_no_title() {
    // given
    UUID ownerId = UUID.randomUUID();
    PlaylistCreateRequest request = new PlaylistCreateRequest("", "설명");

    given(playlistRepository.save(any(Playlist.class)))
        .willThrow(new IllegalArgumentException("제목은 필수입니다."));

    // when & then
    assertThatThrownBy(() -> playlistService.create(request, ownerId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("플레이리스트 수정 성공")
  void update_success() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId)
        .title("원래 제목")
        .description("원래 설명")
        .build();

    PlaylistDto expectedDto = new PlaylistDto(
        playlistId, null, "수정된 제목",
        "수정된 설명", null, 0L, false, List.of()
    );

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistMapper.toDto(playlist)).willReturn(expectedDto);

    // when
    PlaylistDto result = playlistService.update(playlistId, request, ownerId);

    // then
    assertThat(result.title()).isEqualTo("수정된 제목");
    assertThat(result.description()).isEqualTo("수정된 설명");
    verify(playlistMapper).toDto(playlist);
  }

  @Test
  @DisplayName("본인 소유가 아닌 플레이리스트 수정 시 예외 발생")
  void update_fail_forbidden() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");

    Playlist playlist = Playlist.builder()
        .ownerId(otherId)
        .title("원래 제목")
        .description("원래 설명")
        .build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistService.update(playlistId, request, ownerId))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("플레이리스트 삭제 성공")
  void delete_success() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId)
        .title("제목")
        .description("설명")
        .build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

    // when
    playlistService.delete(playlistId, ownerId);

    // then
    verify(playlistRepository).delete(playlist);
  }

  @Test
  @DisplayName("본인 소유가 아닌 플레이리스트 삭제 시 예외 발생")
  void delete_fail_forbidden() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(otherId)
        .title("제목")
        .description("설명")
        .build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistService.delete(playlistId, ownerId))
        .isInstanceOf(PlaylistException.class);
  }
}
