package com.team6.moduply.content.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.content.external.ExternalContentManualImportService;
import com.team6.moduply.content.external.ExternalContentService;
import com.team6.moduply.content.external.sportsdb.SportsDbClient;
import com.team6.moduply.content.external.tmdb.TmdbClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ExternalContentImportController.class,
    // 이 테스트는 HTTP JWT 필터가 아니라 ExternalContentManualImportService의 @PreAuthorize를 검증한다.
    // JwtAuthenticationFilter는 별도 auth 테스트에서 검증하므로 Bean 생성 대상에서 제외한다.
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@Import({
    ExternalContentManualImportService.class,
    ExternalContentImportMethodSecurityTest.MethodSecurityTestConfig.class
})
class ExternalContentImportMethodSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TmdbClient tmdbClient;

  @MockitoBean
  private SportsDbClient sportsDbClient;

  @MockitoBean
  private ExternalContentService externalContentService;

  @Test
  @DisplayName("USER 권한으로 TMDB 영화 수동 수집 API 요청 시 403을 반환한다.")
  void importTmdbMovies_fail_when_requester_is_not_admin() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/contents/import/tmdb/movies")
            .with(user("user@example.com").roles("USER"))
            .with(csrf().asHeader()))
        .andExpect(status().isForbidden());

    verify(tmdbClient, never()).fetchPopularMovies(1, "ko-KR");
  }

  @Test
  @DisplayName("USER 권한으로 TMDB TV 수동 수집 API 요청 시 403을 반환한다.")
  void importTmdbTvSeries_fail_when_requester_is_not_admin() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/contents/import/tmdb/tv-series")
            .with(user("user@example.com").roles("USER"))
            .with(csrf().asHeader()))
        .andExpect(status().isForbidden());

    verify(tmdbClient, never()).fetchPopularTvSeries(1, "ko-KR");
  }

  @Test
  @DisplayName("USER 권한으로 The Sports DB 리그 경기 수동 수집 API 요청 시 403을 반환한다.")
  void importSportsDbLeagueEvents_fail_when_requester_is_not_admin() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/contents/import/sports-db/league-events")
            .param("leagueId", "4328")
            .with(user("user@example.com").roles("USER"))
            .with(csrf().asHeader()))
        .andExpect(status().isForbidden());

    verify(sportsDbClient, never()).fetchNextLeagueEvents("4328");
  }

  @Test
  @DisplayName("USER 권한으로 The Sports DB 일별 경기 수동 수집 API 요청 시 403을 반환한다.")
  void importSportsDbDayEvents_fail_when_requester_is_not_admin() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/contents/import/sports-db/day-events")
            .param("date", "2026-07-02")
            .param("sport", "Soccer")
            .param("leagueId", "4328")
            .with(user("user@example.com").roles("USER"))
            .with(csrf().asHeader()))
        .andExpect(status().isForbidden());

    verify(sportsDbClient, never()).fetchEventsByDay(
        java.time.LocalDate.of(2026, 7, 2),
        "Soccer",
        "4328"
    );
  }

  @EnableMethodSecurity
  static class MethodSecurityTestConfig {
  }
}
