package com.team6.moduply.content.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.sportsdb.SportsDbClient;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventListResponse;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventResponse;
import com.team6.moduply.content.external.tmdb.TmdbClient;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbPageResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalContentImportServiceTest {

  @Mock
  private TmdbClient tmdbClient;

  @Mock
  private SportsDbClient sportsDbClient;

  @Mock
  private ExternalContentService externalContentService;

  @InjectMocks
  private ExternalContentImportService externalContentImportService;

  @Test
  @DisplayName("TMDB 영화 수집 시 TMDB Client 호출 후 외부 콘텐츠 저장 서비스를 호출한다.")
  void importTmdbMovies_success_with_client_response() {
    // Given
    TmdbMovieResponse movie = new TmdbMovieResponse(
        27205L,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "/poster.jpg",
        null,
        "2010-07-15"
    );
    TmdbPageResponse<TmdbMovieResponse> response = new TmdbPageResponse<>(
        1,
        List.of(movie),
        10,
        200
    );
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);

    given(tmdbClient.fetchDiscoverMovies(1, "ko-KR")).willReturn(response);
    given(externalContentService.importTmdbMovies(response.results())).willReturn(expected);

    // When
    ExternalContentImportResult result = externalContentImportService.importTmdbMovies(1, "ko-KR");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(tmdbClient).fetchDiscoverMovies(1, "ko-KR");
    verify(externalContentService).importTmdbMovies(response.results());
  }

  @Test
  @DisplayName("TMDB 영화 응답 results가 null이면 빈 목록으로 저장 서비스를 호출한다.")
  void importTmdbMovies_success_when_results_is_null() {
    // Given
    TmdbPageResponse<TmdbMovieResponse> response = new TmdbPageResponse<>(
        1,
        null,
        10,
        200
    );
    ExternalContentImportResult expected = new ExternalContentImportResult(0, 0, 0, 0, 0);

    given(tmdbClient.fetchDiscoverMovies(1, "ko-KR")).willReturn(response);
    given(externalContentService.importTmdbMovies(List.of())).willReturn(expected);

    // When
    ExternalContentImportResult result = externalContentImportService.importTmdbMovies(1, "ko-KR");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(tmdbClient).fetchDiscoverMovies(1, "ko-KR");
    verify(externalContentService).importTmdbMovies(List.of());
  }

  @Test
  @DisplayName("TMDB TV 수집 시 TMDB Client 호출 후 외부 콘텐츠 저장 서비스를 호출한다.")
  void importTmdbTvSeries_success_with_client_response() {
    // Given
    TmdbTvResponse tv = new TmdbTvResponse(
        1399L,
        "Game of Thrones",
        "Seven noble families fight for control.",
        "/poster.jpg",
        null,
        "2011-04-17"
    );
    TmdbPageResponse<TmdbTvResponse> response = new TmdbPageResponse<>(
        1,
        List.of(tv),
        10,
        200
    );
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);

    given(tmdbClient.fetchDiscoverTvSeries(1, "ko-KR")).willReturn(response);
    given(externalContentService.importTmdbTvSeries(response.results())).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentImportService.importTmdbTvSeries(1, "ko-KR");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(tmdbClient).fetchDiscoverTvSeries(1, "ko-KR");
    verify(externalContentService).importTmdbTvSeries(response.results());
  }

  @Test
  @DisplayName("TMDB TV 응답 results가 null이면 빈 목록으로 저장 서비스를 호출한다.")
  void importTmdbTvSeries_success_when_results_is_null() {
    // Given
    TmdbPageResponse<TmdbTvResponse> response = new TmdbPageResponse<>(
        1,
        null,
        10,
        200
    );
    ExternalContentImportResult expected = new ExternalContentImportResult(0, 0, 0, 0, 0);

    given(tmdbClient.fetchDiscoverTvSeries(1, "ko-KR")).willReturn(response);
    given(externalContentService.importTmdbTvSeries(List.of())).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentImportService.importTmdbTvSeries(1, "ko-KR");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(tmdbClient).fetchDiscoverTvSeries(1, "ko-KR");
    verify(externalContentService).importTmdbTvSeries(List.of());
  }

  @Test
  @DisplayName("The Sports DB 리그 경기 수집 시 저장 서비스를 호출한다.")
  void importSportsDbLeagueEvents_success_with_client_response() {
    // Given
    SportsDbEventResponse event = createSportsEvent();
    SportsDbEventListResponse response = new SportsDbEventListResponse(List.of(event));
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);

    given(sportsDbClient.fetchNextLeagueEvents("4328")).willReturn(response);
    given(externalContentService.importSportsEvents(response.events())).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentImportService.importSportsDbLeagueEvents("4328");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(sportsDbClient).fetchNextLeagueEvents("4328");
    verify(externalContentService).importSportsEvents(response.events());
  }

  @Test
  @DisplayName("The Sports DB 리그 경기 응답 events가 null이면 빈 목록으로 저장 서비스를 호출한다.")
  void importSportsDbLeagueEvents_success_when_events_is_null() {
    // Given
    SportsDbEventListResponse response = new SportsDbEventListResponse(null);
    ExternalContentImportResult expected = new ExternalContentImportResult(0, 0, 0, 0, 0);

    given(sportsDbClient.fetchNextLeagueEvents("4328")).willReturn(response);
    given(externalContentService.importSportsEvents(List.of())).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentImportService.importSportsDbLeagueEvents("4328");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(sportsDbClient).fetchNextLeagueEvents("4328");
    verify(externalContentService).importSportsEvents(List.of());
  }

  @Test
  @DisplayName("The Sports DB 일별 경기 수집 시 저장 서비스를 호출한다.")
  void importSportsDbDayEvents_success_with_client_response() {
    // Given
    LocalDate date = LocalDate.of(2026, 7, 1);
    SportsDbEventResponse event = createSportsEvent();
    SportsDbEventListResponse response = new SportsDbEventListResponse(List.of(event));
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);

    given(sportsDbClient.fetchEventsByDay(date, "Soccer", "4328")).willReturn(response);
    given(externalContentService.importSportsEvents(response.events())).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentImportService.importSportsDbDayEvents(date, "Soccer", "4328");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(sportsDbClient).fetchEventsByDay(date, "Soccer", "4328");
    verify(externalContentService).importSportsEvents(response.events());
  }

  @Test
  @DisplayName("The Sports DB 일별 경기 응답 events가 null이면 빈 목록으로 저장 서비스를 호출한다.")
  void importSportsDbDayEvents_success_when_events_is_null() {
    // Given
    LocalDate date = LocalDate.of(2026, 7, 1);
    SportsDbEventListResponse response = new SportsDbEventListResponse(null);
    ExternalContentImportResult expected = new ExternalContentImportResult(0, 0, 0, 0, 0);

    given(sportsDbClient.fetchEventsByDay(date, "Soccer", "4328")).willReturn(response);
    given(externalContentService.importSportsEvents(List.of())).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentImportService.importSportsDbDayEvents(date, "Soccer", "4328");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(sportsDbClient).fetchEventsByDay(date, "Soccer", "4328");
    verify(externalContentService).importSportsEvents(List.of());
  }

  private SportsDbEventResponse createSportsEvent() {
    return new SportsDbEventResponse(
        "2494000",
        "Arsenal vs Coventry City",
        "",
        "https://r2.thesportsdb.com/images/media/event/thumb/image.jpg",
        "Soccer",
        "English Premier League",
        "2026-08-21"
    );
  }
}
