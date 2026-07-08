package com.team6.moduply.content.batch;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.service.ExternalContentImportService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportsDbContentImportTasklet implements Tasklet {

  private final ExternalContentImportService externalContentImportService;
  private final ContentImportBatchProperties properties;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    int leagueSavedCount = 0;
    int leagueSkippedCount = 0;
    int daySavedCount = 0;
    int daySkippedCount = 0;
    List<String> leagueIds = properties.getSportsDbLeagueIds();
    List<Integer> dayOffsets = properties.getSportsDbDayOffsets();

    for (String leagueId : leagueIds) {
      try {
        ExternalContentImportResult leagueResult =
            externalContentImportService.importSportsDbLeagueEvents(leagueId);
        leagueSavedCount += leagueResult.savedCount();
        leagueSkippedCount += leagueResult.skippedCount();
      } catch (RuntimeException e) {
        log.warn("The Sports DB 리그 경기 수집 실패. leagueId={}", leagueId, e);
      }

      for (Integer dayOffset : dayOffsets) {
        LocalDate targetDate = LocalDate.now().plusDays(dayOffset);
        try {
          ExternalContentImportResult dayResult =
              externalContentImportService.importSportsDbDayEvents(
                  targetDate,
                  properties.getSportsDbSport(),
                  leagueId
              );
          daySavedCount += dayResult.savedCount();
          daySkippedCount += dayResult.skippedCount();
        } catch (RuntimeException e) {
          log.warn("The Sports DB 일별 경기 수집 실패. date={}, sport={}, leagueId={}",
              targetDate, properties.getSportsDbSport(), leagueId, e);
        }
      }
    }

    log.info("The Sports DB 콘텐츠 수집 배치 완료: leagueIds={}, dayOffsets={}, "
            + "leagueSaved={}, leagueSkipped={}, daySaved={}, daySkipped={}",
        leagueIds, dayOffsets, leagueSavedCount, leagueSkippedCount, daySavedCount, daySkippedCount);

    return RepeatStatus.FINISHED;
  }
}
