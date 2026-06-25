package com.team6.moduply.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.playlist.controller.PlaylistController;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.dto.PlaylistUpdateRequest;
import com.team6.moduply.playlist.service.PlaylistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = PlaylistController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = EnableWebSecurity.class)
    })
@ActiveProfiles("test")
class PlaylistControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private PlaylistService playlistService;

  @Test
  @DisplayName("플레이리스트를 생성하면 201을 반환한다.")
  void createPlaylist_success() throws Exception {
    // given
    PlaylistCreateRequest request = new PlaylistCreateRequest("내 최애 영화", "비 오는 날 보기 좋은 영화들");

    PlaylistDto response = new PlaylistDto(
        UUID.randomUUID(),
        null,
        "내 최애 영화",
        "비 오는 날 보기 좋은 영화들",
        null,
        0L,
        false,
        List.of()
    );

    given(playlistService.create(any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/playlists")
            .with(user("test-user").roles("USER"))
            .with(csrf())
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
            .with(user("test-user").roles("USER"))
            .with(csrf())
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

    given(playlistService.update(any(), any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/playlists/" + playlistId)
            .with(user("test-user").roles("USER"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("수정된 제목"))
        .andExpect(jsonPath("$.description").value("수정된 설명"));
  }

  @Test
  @DisplayName("플레이리스트를 삭제하면 204를 반환한다.")
  void deletePlaylist_success() throws Exception {
    // given
    UUID playlistId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/playlists/" + playlistId)
            .with(user("test-user").roles("USER"))
            .with(csrf()))
        .andExpect(status().isNoContent());
  }
}
