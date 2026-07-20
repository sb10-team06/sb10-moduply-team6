package com.team6.moduply.playlist;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.dto.PlaylistSortBy;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.repository.PlaylistRepository;
import com.team6.moduply.playlist.repository.qdsl.PlaylistQDSLRepository;
import com.team6.moduply.playlist.repository.qdsl.PlaylistQDSLRepositoryImpl;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaAuditingConfig.class, PlaylistQDSLRepositoryImpl.class})
class PlaylistRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private PlaylistRepository playlistRepository;

  @Autowired
  private PlaylistQDSLRepository playlistQDSLRepository;

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

  @Test
  @DisplayName("키워드로 플레이리스트를 검색하면 해당 플레이리스트만 반환한다.")
  void findAllWithCursor_success_with_keyword() {
    // given
    playlistRepository.save(Playlist.builder()
        .ownerId(UUID.randomUUID()).title("내 최애 영화").description("설명1").build());
    playlistRepository.save(Playlist.builder()
        .ownerId(UUID.randomUUID()).title("드라마 모음").description("설명2").build());

    PlaylistSearchRequest request = new PlaylistSearchRequest(
        "영화", null, null, null, null, 10,
        SortDirection.DESCENDING, PlaylistSortBy.updatedAt
    );

    // when
    List<Playlist> result = playlistQDSLRepository.findAllWithCursor(request);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("내 최애 영화");
  }

  @Test
  @DisplayName("소유자 ID로 플레이리스트를 검색하면 해당 소유자의 플레이리스트만 반환한다.")
  void findAllWithCursor_success_with_owner_id() {
    // given
    UUID ownerId = UUID.randomUUID();
    playlistRepository.save(Playlist.builder()
        .ownerId(ownerId).title("내 플레이리스트").description("설명").build());
    playlistRepository.save(Playlist.builder()
        .ownerId(UUID.randomUUID()).title("다른 플레이리스트").description("설명").build());

    PlaylistSearchRequest request = new PlaylistSearchRequest(
        null, ownerId, null, null, null, 10,
        SortDirection.DESCENDING, PlaylistSortBy.updatedAt
    );

    // when
    List<Playlist> result = playlistQDSLRepository.findAllWithCursor(request);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOwnerId()).isEqualTo(ownerId);
  }

  @Test
  @DisplayName("전체 플레이리스트 개수를 반환한다.")
  void countWithCondition_success() {
    // given
    playlistRepository.save(Playlist.builder()
        .ownerId(UUID.randomUUID()).title("첫번째").description("설명").build());
    playlistRepository.save(Playlist.builder()
        .ownerId(UUID.randomUUID()).title("두번째").description("설명").build());

    PlaylistSearchRequest request = new PlaylistSearchRequest(
        null, null, null, null, null, 10,
        SortDirection.DESCENDING, PlaylistSortBy.updatedAt
    );

    // when
    long count = playlistQDSLRepository.countWithCondition(request);

    // then
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("limit보다 많은 플레이리스트가 있으면 hasNext를 위해 limit+1개를 반환한다.")
  void findAllWithCursor_success_with_has_next() {
    // given
    for (int i = 0; i < 3; i++) {
      playlistRepository.save(Playlist.builder()
          .ownerId(UUID.randomUUID()).title("플레이리스트" + i).description("설명").build());
    }

    PlaylistSearchRequest request = new PlaylistSearchRequest(
        null, null, null, null, null, 2,
        SortDirection.DESCENDING, PlaylistSortBy.updatedAt
    );

    // when
    List<Playlist> result = playlistQDSLRepository.findAllWithCursor(request);

    // then
    assertThat(result).hasSize(3); // limit+1
  }
}
