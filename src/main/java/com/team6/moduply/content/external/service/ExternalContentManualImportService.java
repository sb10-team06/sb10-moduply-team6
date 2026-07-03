package com.team6.moduply.content.external.service;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalContentManualImportService {

  private final ExternalContentImportService externalContentImportService;

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importTmdbMovies(int page, String language) {
    return externalContentImportService.importTmdbMovies(page, language);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importTmdbTvSeries(int page, String language) {
    return externalContentImportService.importTmdbTvSeries(page, language);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importSportsDbLeagueEvents(String leagueId) {
    return externalContentImportService.importSportsDbLeagueEvents(leagueId);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public ExternalContentImportResult importSportsDbDayEvents(
      LocalDate date,
      String sport,
      String leagueId
  ) {
    return externalContentImportService.importSportsDbDayEvents(date, sport, leagueId);
  }
}
