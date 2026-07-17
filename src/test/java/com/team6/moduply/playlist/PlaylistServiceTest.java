package com.team6.moduply.playlist;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.dto.PlaylistSortBy;
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
import com.team6.moduply.playlist.service.PlaylistService;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static com.team6.moduply.playlist.dto.PlaylistSortBy.updatedAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

  @Mock
  private PlaylistQDSLRepository playlistQDSLRepository;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private PlaylistContentRepository playlistContentRepository;

  @Mock
  private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Mock
  private UserRepository userRepository;

  @Test
  @DisplayName("소유자가 플레이리스트를 생성하면 생성된 플레이리스트를 반환한다.")
  void create_success_with_owner() {
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
    given(playlistMapper.toDto(any(), any(), anyLong(), anyBoolean(), any())).willReturn(expectedDto);
    given(userRepository.findById(ownerId)).willReturn(Optional.of(user("owner")));

    // when
    PlaylistDto result = playlistService.create(request, ownerId);

    // then
    verify(playlistMapper).toDto(eq(savedPlaylist), any(), anyLong(), anyBoolean(), any());
    assertThat(result.title()).isEqualTo("내 최애 영화");
    assertThat(result.description()).isEqualTo("비 오는 날 보기 좋은 영화들");
  }

  @Test
  @DisplayName("제목이 없으면 플레이리스트 생성에 실패한다.")
  void create_fail_with_no_title() {
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
  @DisplayName("소유자가 플레이리스트를 수정하면 수정된 플레이리스트를 반환한다.")
  void update_success_with_owner() {
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
    given(playlistMapper.toDto(any(), any(), anyLong(), anyBoolean(), any())).willReturn(expectedDto);

    // when
    PlaylistDto result = playlistService.update(playlistId, request, ownerId);

    // then
    assertThat(result.title()).isEqualTo("수정된 제목");
    assertThat(result.description()).isEqualTo("수정된 설명");
    assertThat(playlist.getTitle()).isEqualTo("수정된 제목");
    assertThat(playlist.getDescription()).isEqualTo("수정된 설명");
    verify(playlistMapper).toDto(eq(playlist), any(), anyLong(), anyBoolean(), any());
  }

  @Test
  @DisplayName("본인 소유가 아닌 플레이리스트를 수정하면 예외가 발생한다.")
  void update_fail_with_no_permission() {
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
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_FORBIDDEN));
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트를 수정하면 예외가 발생한다.")
  void update_fail_with_not_found_playlist() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");

    given(playlistRepository.findById(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.update(playlistId, request, ownerId))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("소유자가 플레이리스트를 삭제하면 레포지토리의 delete가 호출된다.")
  void delete_success_with_owner() {
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
  @DisplayName("본인 소유가 아닌 플레이리스트를 삭제하면 예외가 발생한다.")
  void delete_fail_with_no_permission() {
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
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_FORBIDDEN));
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트를 삭제하면 예외가 발생한다.")
  void delete_fail_with_not_found_playlist() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.delete(playlistId, ownerId))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("소유자가 플레이리스트를 단건 조회하면 플레이리스트를 반환한다.")
  void findById_success_with_owner() {
    // given
    UUID playlistId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId)
        .title("내 최애 영화")
        .description("비 오는 날 보기 좋은 영화들")
        .build();

    PlaylistDto expectedDto = new PlaylistDto(
        playlistId, null, "내 최애 영화",
        "비 오는 날 보기 좋은 영화들", null, 0L, false, List.of()
    );

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistMapper.toDto(any(), any(), anyLong(), anyBoolean(), any())).willReturn(expectedDto);

    // when
    PlaylistDto result = playlistService.findById(playlistId, null);

    // then
    assertThat(result.title()).isEqualTo("내 최애 영화");
    assertThat(result.description()).isEqualTo("비 오는 날 보기 좋은 영화들");
    verify(playlistMapper).toDto(eq(playlist), any(), anyLong(), anyBoolean(), any());
  }

  @Test
  @DisplayName("플레이리스트를 단건 조회하면 콘텐츠 목록이 포함된다.")
  void findById_success_with_contents() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();
    ReflectionTestUtils.setField(playlist, "id", playlistId);

    PlaylistContent playlistContent = PlaylistContent.builder()
        .playlist(playlist).contentId(contentId).build();

    Content content = new Content(null, "ext-1", ContentType.movie, "영화제목", "설명");
    ReflectionTestUtils.setField(content, "id", contentId);

    User mockUser = new User("test@test.com", "password", "테스트", Role.USER);

    PlaylistDto expectedDto = new PlaylistDto(
        playlistId, null, "제목", "설명", null, 0L, false, List.of()
    );

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(userRepository.findById(ownerId)).willReturn(Optional.of(mockUser));
    given(playlistSubscriptionRepository.countByPlaylist(playlist)).willReturn(0L);
    given(playlistSubscriptionRepository.existsByPlaylistAndSubscriberId(playlist, currentUserId)).willReturn(false);
    given(playlistContentRepository.findAllByPlaylist(playlist)).willReturn(List.of(playlistContent));
    given(contentRepository.findAllById(any())).willReturn(List.of(content));
    given(playlistMapper.toDto(any(), any(), anyLong(), anyBoolean(), any())).willReturn(expectedDto);

    // when
    PlaylistDto result = playlistService.findById(playlistId, currentUserId);

    // then
    assertThat(result).isNotNull();
    verify(playlistContentRepository).findAllByPlaylist(playlist);
    verify(contentRepository).findAllById(any());
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트를 단건 조회하면 예외가 발생한다.")
  void findById_fail_with_not_found_playlist() {
    // given
    UUID playlistId = UUID.randomUUID();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.findById(playlistId, null))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("플레이리스트 목록을 조회하면 커서 응답을 반환한다.")
  void findAll_success() {
    // given
    UUID ownerId = UUID.randomUUID();
    PlaylistSearchRequest request = new PlaylistSearchRequest(
        null, null, null, null, null, 10,
        SortDirection.DESCENDING, updatedAt
    );

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId)
        .title("내 최애 영화")
        .description("비 오는 날 보기 좋은 영화들")
        .build();

    PlaylistDto dto = new PlaylistDto(
        UUID.randomUUID(), null, "내 최애 영화",
        "비 오는 날 보기 좋은 영화들", null, 0L, false, List.of()
    );

    given(playlistQDSLRepository.findAllWithCursor(request)).willReturn(List.of(playlist));
    given(playlistQDSLRepository.countWithCondition(request)).willReturn(1L);
    given(playlistMapper.toDto(any(), any(), anyLong(), anyBoolean(), any())).willReturn(dto);

    // when
    CursorResponse<PlaylistDto> result = playlistService.findAll(request, null);

    // then
    assertThat(result.data()).hasSize(1);
    assertThat(result.totalCount()).isEqualTo(1L);
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  @DisplayName("플레이리스트 목록이 limit개이면 hasNext가 true를 반환한다.")
  void findAll_success_with_has_next() {
    // given
    UUID ownerId = UUID.randomUUID();
    PlaylistSearchRequest request = new PlaylistSearchRequest(
        null, null, null, null, null, 1,
        SortDirection.DESCENDING, PlaylistSortBy.updatedAt
    );

    Playlist playlist1 = Playlist.builder()
        .ownerId(ownerId).title("첫번째").description("설명1").build();
    Playlist playlist2 = Playlist.builder()
        .ownerId(ownerId).title("두번째").description("설명2").build();

    PlaylistDto dto1 = new PlaylistDto(
        UUID.randomUUID(), null, "첫번째", "설명1", null, 0L, false, List.of()
    );

    // limit+1개 반환 (sentinel)
    given(playlistQDSLRepository.findAllWithCursor(request)).willReturn(new ArrayList<>(List.of(playlist1, playlist2)));
    given(playlistQDSLRepository.countWithCondition(request)).willReturn(2L);
    given(playlistMapper.toDto(eq(playlist1), any(), anyLong(), anyBoolean(), any())).willReturn(dto1);

    // when
    CursorResponse<PlaylistDto> result = playlistService.findAll(request, null);

    // then
    assertThat(result.data()).hasSize(1);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("마지막 페이지의 데이터가 limit개이면 hasNext가 false를 반환한다.")
  void findAll_success_with_last_page() {
    // given
    UUID ownerId = UUID.randomUUID();
    PlaylistSearchRequest request = new PlaylistSearchRequest(
        null, null, null, null, null, 2,
        SortDirection.DESCENDING, PlaylistSortBy.updatedAt
    );

    Playlist playlist1 = Playlist.builder()
        .ownerId(ownerId).title("첫번째").description("설명1").build();
    Playlist playlist2 = Playlist.builder()
        .ownerId(ownerId).title("두번째").description("설명2").build();

    PlaylistDto dto1 = new PlaylistDto(
        UUID.randomUUID(), null, "첫번째", "설명1", null, 0L, false, List.of()
    );
    PlaylistDto dto2 = new PlaylistDto(
        UUID.randomUUID(), null, "두번째", "설명2", null, 0L, false, List.of()
    );

    // limit개만 반환 (마지막 페이지)
    given(playlistQDSLRepository.findAllWithCursor(request)).willReturn(List.of(playlist1, playlist2));
    given(playlistQDSLRepository.countWithCondition(request)).willReturn(2L);
    given(playlistMapper.toDto(eq(playlist1), any(), anyLong(), anyBoolean(), any())).willReturn(dto1);
    given(playlistMapper.toDto(eq(playlist2), any(), anyLong(), anyBoolean(), any())).willReturn(dto2);

    // when
    CursorResponse<PlaylistDto> result = playlistService.findAll(request, null);

    // then
    assertThat(result.data()).hasSize(2);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
  }

  @Test
  @DisplayName("소유자가 플레이리스트에 콘텐츠를 추가하면 저장된다.")
  void addContent_success_with_owner() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();

    given(contentRepository.existsById(contentId)).willReturn(true);
    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistContentRepository.existsByPlaylistAndContentId(playlist, contentId)).willReturn(false);
    given(userRepository.findById(ownerId)).willReturn(Optional.of(user("owner")));

    // when
    playlistService.addContent(playlistId, contentId, ownerId);

    // then
    verify(playlistContentRepository).save(any(PlaylistContent.class));
  }

  @Test
  @DisplayName("이미 추가된 콘텐츠를 추가하면 예외가 발생한다.")
  void addContent_fail_with_duplicate() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(contentRepository.existsById(contentId)).willReturn(true);
    given(playlistContentRepository.existsByPlaylistAndContentId(playlist, contentId)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> playlistService.addContent(playlistId, contentId, ownerId))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트에 콘텐츠를 추가하면 예외가 발생한다.")
  void addContent_fail_with_not_found_playlist() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.addContent(playlistId, contentId, ownerId))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠를 추가하면 예외가 발생한다.")
  void addContent_fail_with_not_found_content() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(contentRepository.existsById(contentId)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> playlistService.addContent(playlistId, contentId, ownerId))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("본인 소유가 아닌 플레이리스트에 콘텐츠를 추가하면 예외가 발생한다.")
  void addContent_fail_with_no_permission() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(otherId).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistService.addContent(playlistId, contentId, ownerId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_FORBIDDEN));
  }

  @Test
  @DisplayName("소유자가 플레이리스트에서 콘텐츠를 삭제하면 레포지토리의 delete가 호출된다.")
  void removeContent_success_with_owner() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();

    PlaylistContent playlistContent = PlaylistContent.builder()
        .playlist(playlist).contentId(contentId).build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistContentRepository.findByPlaylistAndContentId(playlist, contentId))
        .willReturn(Optional.of(playlistContent));

    // when
    playlistService.removeContent(playlistId, contentId, ownerId);

    // then
    verify(playlistContentRepository).delete(playlistContent);
  }

  @Test
  @DisplayName("플레이리스트에 없는 콘텐츠를 삭제하면 예외가 발생한다.")
  void removeContent_fail_with_not_found_content() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistContentRepository.findByPlaylistAndContentId(playlist, contentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.removeContent(playlistId, contentId, ownerId))
        .isInstanceOf(PlaylistException.class);
  }

  @Test
  @DisplayName("본인 소유가 아닌 플레이리스트에서 콘텐츠를 삭제하면 예외가 발생한다.")
  void removeContent_fail_with_no_permission() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(otherId).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistService.removeContent(playlistId, contentId, ownerId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_FORBIDDEN));
  }

  @Test
  @DisplayName("플레이리스트를 구독하면 구독 정보가 저장된다.")
  void subscribe_success() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    User mockUser = new User("test@test.com", "password", "테스트", Role.USER);
    given(userRepository.findById(subscriberId)).willReturn(Optional.of(mockUser));

    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID()).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistSubscriptionRepository.existsByPlaylistAndSubscriberId(playlist, subscriberId))
        .willReturn(false);

    // when
    playlistService.subscribe(playlistId, subscriberId);

    // then
    verify(playlistSubscriptionRepository).save(any(PlaylistSubscription.class));
  }

  @Test
  @DisplayName("구독 중인 플레이리스트 목록 조회 시 subscribedByMe가 true를 반환한다.")
  void findAll_success_with_subscribed_by_me() {
    // given
    UUID currentUserId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    PlaylistSearchRequest request = new PlaylistSearchRequest(
        null, null, null, null, null, 10,
        SortDirection.DESCENDING, PlaylistSortBy.updatedAt
    );

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();
    ReflectionTestUtils.setField(playlist, "id", playlistId);

    PlaylistDto dto = new PlaylistDto(
        playlistId, null, "제목", "설명", null, 0L, true, List.of()
    );

    given(playlistQDSLRepository.findAllWithCursor(request)).willReturn(List.of(playlist));
    given(playlistQDSLRepository.countWithCondition(request)).willReturn(1L);
    given(userRepository.findAllByIdWithProfileImg(any())).willReturn(List.of());
    given(playlistSubscriptionRepository.countByPlaylistIds(any())).willReturn(List.of());
    given(playlistContentRepository.findAllByPlaylistIn(any())).willReturn(List.of());
    given(playlistSubscriptionRepository.findSubscribedPlaylistIdsBySubscriberIdAndPlaylistIdIn(
        eq(currentUserId), any())).willReturn(List.of(playlistId));
    given(playlistMapper.toDto(any(), any(), anyLong(), eq(true), any())).willReturn(dto);

    // when
    CursorResponse<PlaylistDto> result = playlistService.findAll(request, currentUserId);

    // then
    assertThat(result.data().get(0).subscribedByMe()).isTrue();
  }

  @Test
  @DisplayName("본인 플레이리스트를 구독하면 예외가 발생한다.")
  void subscribe_fail_with_self_subscription() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(ownerId).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistService.subscribe(playlistId, ownerId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_SELF_SUBSCRIPTION));
  }

  @Test
  @DisplayName("이미 구독 중인 플레이리스트를 구독하면 예외가 발생한다.")
  void subscribe_fail_with_duplicate() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID()).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistSubscriptionRepository.existsByPlaylistAndSubscriberId(playlist, subscriberId))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> playlistService.subscribe(playlistId, subscriberId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS));
  }

  @Test
  @DisplayName("구독 동시 요청으로 DataIntegrityViolationException 발생 시 409를 반환한다.")
  void subscribe_fail_with_data_integrity_violation() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID()).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistSubscriptionRepository.existsByPlaylistAndSubscriberId(playlist, subscriberId))
        .willReturn(false);
    given(playlistSubscriptionRepository.save(any()))
        .willThrow(new DataIntegrityViolationException("duplicate"));

    // when & then
    assertThatThrownBy(() -> playlistService.subscribe(playlistId, subscriberId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS));
  }

  @Test
  @DisplayName("플레이리스트 구독을 취소하면 구독 정보가 삭제된다.")
  void unsubscribe_success() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID()).title("제목").description("설명").build();

    PlaylistSubscription subscription = PlaylistSubscription.builder()
        .playlist(playlist).subscriberId(subscriberId).build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistSubscriptionRepository.findByPlaylistAndSubscriberId(playlist, subscriberId))
        .willReturn(Optional.of(subscription));

    // when
    playlistService.unsubscribe(playlistId, subscriberId);

    // then
    verify(playlistSubscriptionRepository).delete(subscription);
  }

  @Test
  @DisplayName("구독하지 않은 플레이리스트를 구독취소하면 예외가 발생한다.")
  void unsubscribe_fail_with_not_found() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID()).title("제목").description("설명").build();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
    given(playlistSubscriptionRepository.findByPlaylistAndSubscriberId(playlist, subscriberId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.unsubscribe(playlistId, subscriberId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND));
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트를 구독하면 예외가 발생한다.")
  void subscribe_fail_with_not_found_playlist() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.subscribe(playlistId, subscriberId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_NOT_FOUND));
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트를 구독취소하면 예외가 발생한다.")
  void unsubscribe_fail_with_not_found_playlist() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    given(playlistRepository.findById(playlistId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistService.unsubscribe(playlistId, subscriberId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(e -> assertThat(((PlaylistException) e).getErrorCode())
            .isEqualTo(PlaylistErrorCode.PLAYLIST_NOT_FOUND));
  }

  private User user(String name) {
    return new User(name + "@example.com", "password", name, Role.USER);
  }
}
