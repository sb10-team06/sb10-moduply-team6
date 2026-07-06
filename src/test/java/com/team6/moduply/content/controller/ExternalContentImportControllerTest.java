package com.team6.moduply.content.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.service.ExternalContentManualImportService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ExternalContentImportController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
class ExternalContentImportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ExternalContentManualImportService externalContentManualImportService;

  @Test
  @DisplayName("TMDB 영화 수동 수집 API 호출 시 수집 결과를 반환한다.")
  void importTmdbMovies_success_with_request_parameters() throws Exception {
    // Given
    ExternalContentImportResult response = new ExternalContentImportResult(20, 18, 2, 15, 3);
    given(externalContentManualImportService.importTmdbMovies(2, "ko-KR"))
        .willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/contents/import/tmdb/movies")
            .param("page", "2")
            .param("language", "ko-KR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestedCount").value(20))
        .andExpect(jsonPath("$.savedCount").value(18))
        .andExpect(jsonPath("$.skippedCount").value(2))
        .andExpect(jsonPath("$.imageSavedCount").value(15))
        .andExpect(jsonPath("$.imageFailedCount").value(3));

    verify(externalContentManualImportService).importTmdbMovies(2, "ko-KR");
  }

  @Test
  @DisplayName("TMDB TV 수동 수집 API 호출 시 기본 페이지와 언어로 수집한다.")
  void importTmdbTvSeries_success_with_default_parameters() throws Exception {
    // Given
    ExternalContentImportResult response = new ExternalContentImportResult(20, 20, 0, 20, 0);
    given(externalContentManualImportService.importTmdbTvSeries(1, "ko-KR"))
        .willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/contents/import/tmdb/tv-series"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestedCount").value(20))
        .andExpect(jsonPath("$.savedCount").value(20));

    verify(externalContentManualImportService).importTmdbTvSeries(1, "ko-KR");
  }

  @Test
  @DisplayName("The Sports DB 리그 경기 수동 수집 API 호출 시 수집 결과를 반환한다.")
  void importSportsDbLeagueEvents_success_with_league_id() throws Exception {
    // Given
    ExternalContentImportResult response = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(externalContentManualImportService.importSportsDbLeagueEvents("4328"))
        .willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/contents/import/sports-db/league-events")
            .param("leagueId", "4328"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.savedCount").value(1));

    verify(externalContentManualImportService).importSportsDbLeagueEvents("4328");
  }

  @Test
  @DisplayName("The Sports DB 리그 ID가 공백이면 400 응답을 반환한다.")
  void importSportsDbLeagueEvents_fail_when_league_id_is_blank() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/contents/import/sports-db/league-events")
            .param("leagueId", " "))
        .andExpect(status().isBadRequest());

    verify(externalContentManualImportService, never()).importSportsDbLeagueEvents(anyString());
  }

  @Test
  @DisplayName("The Sports DB 일별 경기 수동 수집 API 호출 시 수집 결과를 반환한다.")
  void importSportsDbDayEvents_success_with_day_parameters() throws Exception {
    // Given
    LocalDate date = LocalDate.of(2026, 7, 1);
    ExternalContentImportResult response = new ExternalContentImportResult(3, 2, 1, 2, 0);
    given(externalContentManualImportService.importSportsDbDayEvents(date, "Soccer", "4328"))
        .willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/contents/import/sports-db/day-events")
            .param("date", "2026-07-01")
            .param("sport", "Soccer")
            .param("leagueId", "4328"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestedCount").value(3))
        .andExpect(jsonPath("$.savedCount").value(2))
        .andExpect(jsonPath("$.skippedCount").value(1));

    verify(externalContentManualImportService).importSportsDbDayEvents(date, "Soccer", "4328");
  }
}
