package com.team6.moduply.content.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalContentManualImportServiceTest {

  @Mock
  private ExternalContentImportService externalContentImportService;

  @InjectMocks
  private ExternalContentManualImportService externalContentManualImportService;

  @Test
  @DisplayName("TMDB 영화 수동 수집 시 외부 콘텐츠 수집 서비스에 위임한다.")
  void importTmdbMovies_success_delegate_import_service() {
    // Given
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(externalContentImportService.importTmdbMovies(1, "ko-KR")).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentManualImportService.importTmdbMovies(1, "ko-KR");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(externalContentImportService).importTmdbMovies(1, "ko-KR");
  }

  @Test
  @DisplayName("TMDB TV 수동 수집 시 외부 콘텐츠 수집 서비스에 위임한다.")
  void importTmdbTvSeries_success_delegate_import_service() {
    // Given
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(externalContentImportService.importTmdbTvSeries(1, "ko-KR")).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentManualImportService.importTmdbTvSeries(1, "ko-KR");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(externalContentImportService).importTmdbTvSeries(1, "ko-KR");
  }

  @Test
  @DisplayName("The Sports DB 리그 경기 수동 수집 시 외부 콘텐츠 수집 서비스에 위임한다.")
  void importSportsDbLeagueEvents_success_delegate_import_service() {
    // Given
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(externalContentImportService.importSportsDbLeagueEvents("4328")).willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentManualImportService.importSportsDbLeagueEvents("4328");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(externalContentImportService).importSportsDbLeagueEvents("4328");
  }

  @Test
  @DisplayName("The Sports DB 일별 경기 수동 수집 시 외부 콘텐츠 수집 서비스에 위임한다.")
  void importSportsDbDayEvents_success_delegate_import_service() {
    // Given
    LocalDate date = LocalDate.of(2026, 7, 1);
    ExternalContentImportResult expected = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(externalContentImportService.importSportsDbDayEvents(date, "Soccer", "4328"))
        .willReturn(expected);

    // When
    ExternalContentImportResult result =
        externalContentManualImportService.importSportsDbDayEvents(date, "Soccer", "4328");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(externalContentImportService).importSportsDbDayEvents(date, "Soccer", "4328");
  }
}
