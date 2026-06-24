package com.team6.moduply.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.playlist.controller.PlaylistController;
import com.team6.moduply.playlist.dto.PlaylistCreateRequest;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.service.PlaylistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaylistController.class)
class PlaylistControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private PlaylistService playlistService;

  @Test
  @DisplayName("플레이리스트 생성 성공 - 201 반환")
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
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("내 최애 영화"))
        .andExpect(jsonPath("$.description").value("비 오는 날 보기 좋은 영화들"));
  }

  @Test
  @DisplayName("제목 없이 플레이리스트 생성 시 400 반환")
  void createPlaylist_fail_no_title() throws Exception {
    // given
    PlaylistCreateRequest request = new PlaylistCreateRequest("", "설명");

    // when & then
    mockMvc.perform(post("/api/playlists")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
