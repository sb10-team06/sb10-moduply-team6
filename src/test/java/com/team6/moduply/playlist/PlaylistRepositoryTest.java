package com.team6.moduply.playlist;

import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.repository.PlaylistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private PlaylistRepository playlistRepository;

  @Test
  @DisplayName("플레이리스트 저장 성공")
  void save_success() {
    // given
    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID())
        .title("내 최애 영화")
        .description("비 오는 날 보기 좋은 영화들")
        .build();

    // when
    Playlist saved = playlistRepository.save(playlist);

    // then
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTitle()).isEqualTo("내 최애 영화");
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("플레이리스트 ID로 조회 성공")
  void findById_success() {
    // given
    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID())
        .title("내 최애 영화")
        .description("비 오는 날 보기 좋은 영화들")
        .build();
    Playlist saved = playlistRepository.save(playlist);

    // when
    Playlist found = playlistRepository.findById(saved.getId()).orElseThrow();

    // then
    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getTitle()).isEqualTo("내 최애 영화");
  }
}
