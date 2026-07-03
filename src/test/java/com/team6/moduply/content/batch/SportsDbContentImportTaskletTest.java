package com.team6.moduply.content.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.service.ExternalContentImportService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class SportsDbContentImportTaskletTest {

  @Mock
  private ExternalContentImportService externalContentImportService;

  @Mock
  private ContentImportBatchProperties properties;

  @InjectMocks
  private SportsDbContentImportTasklet tasklet;

  @Test
  @DisplayName("The Sports DB 콘텐츠 수집 Tasklet 실행 시 리그 경기와 일별 경기를 수집한다.")
  void execute_success_import_sports_db_contents() throws Exception {
    // Given
    ExternalContentImportResult result = new ExternalContentImportResult(1, 1, 0, 1, 0);
    LocalDate today = LocalDate.now();
    given(properties.getSportsDbLeagueIds()).willReturn(List.of("4328", "4335"));
    given(properties.getSportsDbDayOffsets()).willReturn(List.of(-1, 0, 1));
    given(properties.getSportsDbSport()).willReturn("Soccer");
    given(externalContentImportService.importSportsDbLeagueEvents("4328")).willReturn(result);
    given(externalContentImportService.importSportsDbLeagueEvents("4335")).willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(
        eq(today.minusDays(1)),
        eq("Soccer"),
        eq("4328")
    )).willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(eq(today), eq("Soccer"), eq("4328")))
        .willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(
        eq(today.plusDays(1)),
        eq("Soccer"),
        eq("4328")
    )).willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(
        eq(today.minusDays(1)),
        eq("Soccer"),
        eq("4335")
    )).willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(eq(today), eq("Soccer"), eq("4335")))
        .willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(
        eq(today.plusDays(1)),
        eq("Soccer"),
        eq("4335")
    )).willReturn(result);

    // When
    RepeatStatus status = tasklet.execute(null, null);

    // Then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(externalContentImportService).importSportsDbLeagueEvents("4328");
    verify(externalContentImportService).importSportsDbLeagueEvents("4335");
    verify(externalContentImportService).importSportsDbDayEvents(today.minusDays(1), "Soccer", "4328");
    verify(externalContentImportService).importSportsDbDayEvents(today, "Soccer", "4328");
    verify(externalContentImportService).importSportsDbDayEvents(today.plusDays(1), "Soccer", "4328");
    verify(externalContentImportService).importSportsDbDayEvents(today.minusDays(1), "Soccer", "4335");
    verify(externalContentImportService).importSportsDbDayEvents(today, "Soccer", "4335");
    verify(externalContentImportService).importSportsDbDayEvents(today.plusDays(1), "Soccer", "4335");
  }

  @Test
  @DisplayName("The Sports DB 콘텐츠 수집 중 일부 리그가 실패해도 다음 리그 수집을 계속한다.")
  void execute_success_continue_when_sports_db_league_fails() throws Exception {
    // Given
    ExternalContentImportResult result = new ExternalContentImportResult(1, 1, 0, 1, 0);
    LocalDate today = LocalDate.now();
    given(properties.getSportsDbLeagueIds()).willReturn(List.of("4328", "4335"));
    given(properties.getSportsDbDayOffsets()).willReturn(List.of(0));
    given(properties.getSportsDbSport()).willReturn("Soccer");
    willThrow(new RuntimeException("sports league failed"))
        .given(externalContentImportService)
        .importSportsDbLeagueEvents("4328");
    given(externalContentImportService.importSportsDbDayEvents(today, "Soccer", "4328"))
        .willReturn(result);
    given(externalContentImportService.importSportsDbLeagueEvents("4335")).willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(today, "Soccer", "4335"))
        .willReturn(result);

    // When
    RepeatStatus status = tasklet.execute(null, null);

    // Then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(externalContentImportService).importSportsDbLeagueEvents("4328");
    verify(externalContentImportService).importSportsDbDayEvents(today, "Soccer", "4328");
    verify(externalContentImportService).importSportsDbLeagueEvents("4335");
    verify(externalContentImportService).importSportsDbDayEvents(today, "Soccer", "4335");
  }

  @Test
  @DisplayName("The Sports DB 일별 수집이 실패해도 다음 일자와 리그 수집을 계속한다.")
  void execute_success_continue_when_sports_db_day_fails() throws Exception {
    // Given
    ExternalContentImportResult result = new ExternalContentImportResult(1, 1, 0, 1, 0);
    LocalDate today = LocalDate.now();
    given(properties.getSportsDbLeagueIds()).willReturn(List.of("4328", "4335"));
    given(properties.getSportsDbDayOffsets()).willReturn(List.of(0, 1));
    given(properties.getSportsDbSport()).willReturn("Soccer");
    given(externalContentImportService.importSportsDbLeagueEvents("4328")).willReturn(result);
    willThrow(new RuntimeException("sports day failed"))
        .given(externalContentImportService)
        .importSportsDbDayEvents(today, "Soccer", "4328");
    given(externalContentImportService.importSportsDbDayEvents(today.plusDays(1), "Soccer", "4328"))
        .willReturn(result);
    given(externalContentImportService.importSportsDbLeagueEvents("4335")).willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(today, "Soccer", "4335"))
        .willReturn(result);
    given(externalContentImportService.importSportsDbDayEvents(today.plusDays(1), "Soccer", "4335"))
        .willReturn(result);

    // When
    RepeatStatus status = tasklet.execute(null, null);

    // Then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(externalContentImportService).importSportsDbLeagueEvents("4328");
    verify(externalContentImportService).importSportsDbDayEvents(today, "Soccer", "4328");
    verify(externalContentImportService).importSportsDbDayEvents(today.plusDays(1), "Soccer", "4328");
    verify(externalContentImportService).importSportsDbLeagueEvents("4335");
    verify(externalContentImportService).importSportsDbDayEvents(today, "Soccer", "4335");
    verify(externalContentImportService).importSportsDbDayEvents(today.plusDays(1), "Soccer", "4335");
  }
}
