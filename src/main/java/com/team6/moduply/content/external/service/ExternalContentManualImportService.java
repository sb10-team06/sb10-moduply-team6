package com.team6.moduply.content.external.service;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExternalContentManualImportService {

  private final ExternalContentImportService externalContentImportService;

  public ExternalContentImportResult importTmdbMovies(int page, String language) {
    return externalContentImportService.importTmdbMovies(page, language);
  }

  public ExternalContentImportResult importTmdbTvSeries(int page, String language) {
    return externalContentImportService.importTmdbTvSeries(page, language);
  }

  public ExternalContentImportResult importSportsDbLeagueEvents(String leagueId) {
    return externalContentImportService.importSportsDbLeagueEvents(leagueId);
  }

  public ExternalContentImportResult importSportsDbDayEvents(
      LocalDate date,
      String sport,
      String leagueId
  ) {
    return externalContentImportService.importSportsDbDayEvents(date, sport, leagueId);
  }
}
