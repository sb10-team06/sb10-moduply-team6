package com.team6.moduply.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.playlist.controller.PlaylistController;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.exception.PlaylistErrorCode;
import com.team6.moduply.playlist.exception.PlaylistException;
import com.team6.moduply.playlist.service.PlaylistService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = PlaylistController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class PlaylistControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private PlaylistService playlistService;

  private static final UUID TEST_OWNER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    ModuPlyUserDetails userDetails = createUserDetails();
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ModuPlyUserDetails createUserDetails() {
    UserDto userDto = new UserDto(
        TEST_OWNER_ID, Instant.now(), "test@test.com",
        "테스트", null, Role.USER, false
    );
    return new ModuPlyUserDetails(userDto, "password");
  }

  @Test
  @DisplayName("플레이리스트를 생성하면 201을 반환한다.")
  void createPlaylist_success() throws Exception {
    // given
    PlaylistCreateRequest request = new PlaylistCreateRequest("내 최애 영화", "비 오는 날 보기 좋은 영화들");

    PlaylistDto response = new PlaylistDto(
        UUID.randomUUID(), null, "내 최애 영화",
        "비 오는 날 보기 좋은 영화들", null, 0L, false, List.of()
    );

    given(playlistService.create(any(), eq(TEST_OWNER_ID))).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/playlists")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("내 최애 영화"))
        .andExpect(jsonPath("$.description").value("비 오는 날 보기 좋은 영화들"));
  }

  @Test
  @DisplayName("제목 없이 플레이리스트를 생성하면 400을 반환한다.")
  void createPlaylist_fail_with_no_title() throws Exception {
    // given
    PlaylistCreateRequest request = new PlaylistCreateRequest("", "설명");

    // when & then
    mockMvc.perform(post("/api/playlists")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("플레이리스트를 수정하면 200을 반환한다.")
  void updatePlaylist_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();
    PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");

    PlaylistDto response = new PlaylistDto(
        playlistId, null, "수정된 제목",
        "수정된 설명", null, 0L, false, List.of()
    );

    given(playlistService.update(eq(playlistId), any(PlaylistUpdateRequest.class), eq(TEST_OWNER_ID)))
        .willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/playlists/" + playlistId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("수정된 제목"))
        .andExpect(jsonPath("$.description").value("수정된 설명"));

    verify(playlistService).update(eq(playlistId), any(PlaylistUpdateRequest.class), eq(TEST_OWNER_ID));
  }

  @Test
  @DisplayName("다른 사용자의 플레이리스트를 수정하면 403을 반환한다.")
  void updatePlaylist_fail_with_forbidden() throws Exception {
    UUID playlistId = UUID.randomUUID();

    given(playlistService.update(eq(playlistId), any(PlaylistUpdateRequest.class), eq(TEST_OWNER_ID)))
        .willThrow(new PlaylistException(
            PlaylistErrorCode.PLAYLIST_FORBIDDEN,
            Map.of("playlistId", playlistId)
        ));

    mockMvc.perform(patch("/api/playlists/" + playlistId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PlaylistUpdateRequest("제목", "설명"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("플레이리스트를 삭제하면 204를 반환한다.")
  void deletePlaylist_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/playlists/" + playlistId))
        .andExpect(status().isNoContent());

    verify(playlistService).delete(eq(playlistId), eq(TEST_OWNER_ID));
  }

  @Test
  @DisplayName("다른 사용자의 플레이리스트를 삭제하면 403을 반환한다.")
  void deletePlaylist_fail_with_forbidden() throws Exception {
    UUID playlistId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_FORBIDDEN,
        Map.of("playlistId", playlistId)
    )).when(playlistService).delete(eq(playlistId), eq(TEST_OWNER_ID));

    mockMvc.perform(delete("/api/playlists/" + playlistId))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("플레이리스트를 단건 조회하면 200을 반환한다.")
  void getPlaylist_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    PlaylistDto response = new PlaylistDto(
        playlistId, null, "내 최애 영화",
        "비 오는 날 보기 좋은 영화들", null, 0L, false, List.of()
    );

    given(playlistService.findById(any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/playlists/" + playlistId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("내 최애 영화"))
        .andExpect(jsonPath("$.description").value("비 오는 날 보기 좋은 영화들"));
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트를 조회하면 404를 반환한다.")
  void getPlaylist_fail_with_not_found() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    given(playlistService.findById(any()))
        .willThrow(new PlaylistException(
            PlaylistErrorCode.PLAYLIST_NOT_FOUND,
            Map.of("playlistId", playlistId)
        ));

    // when & then
    mockMvc.perform(get("/api/playlists/" + playlistId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("플레이리스트 목록을 조회하면 200을 반환한다.")
  void getPlaylists_success() throws Exception {
    // given
    CursorResponse<PlaylistDto> response = new CursorResponse<>(
        List.of(), null, null, false, 0L, "updatedAt", SortDirection.DESCENDING
    );

    given(playlistService.findAll(any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/playlists")
            .param("limit", "10")
            .param("sortDirection", "DESCENDING")
            .param("sortBy", "updatedAt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(0));
  }

  @Test
  @DisplayName("플레이리스트에 콘텐츠를 추가하면 201을 반환한다.")
  void addContent_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/playlists/" + playlistId + "/contents/" + contentId))
        .andExpect(status().isCreated());

    verify(playlistService).addContent(eq(playlistId), eq(contentId), eq(TEST_OWNER_ID));
  }

  @Test
  @DisplayName("다른 사용자의 플레이리스트에 콘텐츠를 추가하면 403을 반환한다.")
  void addContent_fail_with_forbidden() throws Exception {
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_FORBIDDEN,
        Map.of("playlistId", playlistId)
    )).when(playlistService).addContent(eq(playlistId), eq(contentId), eq(TEST_OWNER_ID));

    mockMvc.perform(post("/api/playlists/" + playlistId + "/contents/" + contentId))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("플레이리스트에서 콘텐츠를 삭제하면 204를 반환한다.")
  void removeContent_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/playlists/" + playlistId + "/contents/" + contentId))
        .andExpect(status().isNoContent());

    verify(playlistService).removeContent(eq(playlistId), eq(contentId), eq(TEST_OWNER_ID));
  }

  @Test
  @DisplayName("중복 콘텐츠를 추가하면 409를 반환한다.")
  void addContent_fail_with_duplicate() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS,
        Map.of("playlistId", playlistId, "contentId", contentId)
    )).when(playlistService).addContent(eq(playlistId), eq(contentId), eq(TEST_OWNER_ID));

    // when & then
    mockMvc.perform(post("/api/playlists/" + playlistId + "/contents/" + contentId))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("존재하지 않는 플레이리스트에 콘텐츠를 추가하면 404를 반환한다.")
  void addContent_fail_with_not_found() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_NOT_FOUND,
        Map.of("playlistId", playlistId)
    )).when(playlistService).addContent(eq(playlistId), eq(contentId), eq(TEST_OWNER_ID));

    // when & then
    mockMvc.perform(post("/api/playlists/" + playlistId + "/contents/" + contentId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("플레이리스트에 없는 콘텐츠를 삭제하면 404를 반환한다.")
  void removeContent_fail_with_not_found() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_CONTENT_NOT_FOUND,
        Map.of("playlistId", playlistId, "contentId", contentId)
    )).when(playlistService).removeContent(eq(playlistId), eq(contentId), eq(TEST_OWNER_ID));

    // when & then
    mockMvc.perform(delete("/api/playlists/" + playlistId + "/contents/" + contentId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("다른 사용자의 플레이리스트에서 콘텐츠를 삭제하면 403을 반환한다.")
  void removeContent_fail_with_forbidden() throws Exception {
    UUID playlistId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_FORBIDDEN,
        Map.of("playlistId", playlistId)
    )).when(playlistService).removeContent(eq(playlistId), eq(contentId), eq(TEST_OWNER_ID));

    mockMvc.perform(delete("/api/playlists/" + playlistId + "/contents/" + contentId))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("플레이리스트를 구독하면 201을 반환한다.")
  void subscribe_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/playlists/" + playlistId + "/subscriptions"))
        .andExpect(status().isCreated());

    verify(playlistService).subscribe(eq(playlistId), eq(TEST_OWNER_ID));
  }

  @Test
  @DisplayName("본인 플레이리스트를 구독하면 400을 반환한다.")
  void subscribe_fail_with_self_subscription() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_SELF_SUBSCRIPTION,
        Map.of("playlistId", playlistId)
    )).when(playlistService).subscribe(eq(playlistId), eq(TEST_OWNER_ID));

    // when & then
    mockMvc.perform(post("/api/playlists/" + playlistId + "/subscriptions"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("이미 구독 중인 플레이리스트를 구독하면 409를 반환한다.")
  void subscribe_fail_with_duplicate() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS,
        Map.of("playlistId", playlistId)
    )).when(playlistService).subscribe(eq(playlistId), eq(TEST_OWNER_ID));

    // when & then
    mockMvc.perform(post("/api/playlists/" + playlistId + "/subscriptions"))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("플레이리스트 구독을 취소하면 204를 반환한다.")
  void unsubscribe_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/playlists/" + playlistId + "/subscriptions"))
        .andExpect(status().isNoContent());

    verify(playlistService).unsubscribe(eq(playlistId), eq(TEST_OWNER_ID));
  }

  @Test
  @DisplayName("구독하지 않은 플레이리스트를 구독취소하면 404를 반환한다.")
  void unsubscribe_fail_with_not_found() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    doThrow(new PlaylistException(
        PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND,
        Map.of("playlistId", playlistId)
    )).when(playlistService).unsubscribe(eq(playlistId), eq(TEST_OWNER_ID));

    // when & then
    mockMvc.perform(delete("/api/playlists/" + playlistId + "/subscriptions"))
        .andExpect(status().isNotFound());
  }
}