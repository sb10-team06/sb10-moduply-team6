package com.team6.moduply.content.external;

import com.team6.moduply.content.external.sportsdb.SportsDbClient;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventListResponse;
import com.team6.moduply.content.external.tmdb.TmdbClient;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbPageResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalContentManualImportService {

  private final TmdbClient tmdbClient;
  private final SportsDbClient sportsDbClient;
  private final ExternalContentService externalContentService;

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importTmdbMovies(int page, String language) {
    TmdbPageResponse<TmdbMovieResponse> response = tmdbClient.fetchPopularMovies(page, language);
    return externalContentService.importTmdbMovies(emptyIfNull(response.results()));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importTmdbTvSeries(int page, String language) {
    TmdbPageResponse<TmdbTvResponse> response = tmdbClient.fetchPopularTvSeries(page, language);
    return externalContentService.importTmdbTvSeries(emptyIfNull(response.results()));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importSportsDbLeagueEvents(String leagueId) {
    SportsDbEventListResponse response = sportsDbClient.fetchNextLeagueEvents(leagueId);
    return externalContentService.importSportsEvents(emptyIfNull(response.events()));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importSportsDbDayEvents(
      LocalDate date,
      String sport,
      String leagueId
  ) {
    SportsDbEventListResponse response = sportsDbClient.fetchEventsByDay(date, sport, leagueId);
    return externalContentService.importSportsEvents(emptyIfNull(response.events()));
  }

  private <T> List<T> emptyIfNull(List<T> values) {
    return values == null ? List.of() : values;
  }
}
