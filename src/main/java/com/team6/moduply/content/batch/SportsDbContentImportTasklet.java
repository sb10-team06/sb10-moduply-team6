package com.team6.moduply.content.batch;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.service.ExternalContentImportService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportsDbContentImportTasklet implements Tasklet {

  private static final String IMPORT_MODE_INITIAL = "INITIAL";
  private static final String IMPORT_MODE_PARAMETER = "importMode";

  private final ExternalContentImportService externalContentImportService;
  private final ContentImportBatchProperties properties;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    boolean initialImport = isInitialImport(chunkContext);
    int daySavedCount = 0;
    int daySkippedCount = 0;
    int requestedCount = 0;
    int succeededCount = 0;
    int failedCount = 0;
    List<String> leagueIds = properties.getSportsDbLeagueIds().stream()
        .map(String::trim)
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
    List<Integer> dayOffsets = getDayOffsets(initialImport);

    for (String leagueId : leagueIds) {
      for (Integer dayOffset : dayOffsets) {
        LocalDate targetDate = LocalDate.now().plusDays(dayOffset);
        try {
          requestedCount++;
          ExternalContentImportResult dayResult =
              externalContentImportService.importSportsDbDayEvents(
                  targetDate,
                  properties.getSportsDbSport(),
                  leagueId
              );
          succeededCount++;
          daySavedCount += dayResult.savedCount();
          daySkippedCount += dayResult.skippedCount();
        } catch (RuntimeException e) {
          failedCount++;
          log.warn("The Sports DB 일별 경기 수집 실패. date={}, sport={}, leagueId={}",
              targetDate, properties.getSportsDbSport(), leagueId, e);
        } finally {
          delayNextRequest();
        }
      }
    }

    log.info("The Sports DB 콘텐츠 수집 배치 완료: initialImport={}, leagueIds={}, dayOffsets={}, "
            + "daySaved={}, daySkipped={}, requested={}, succeeded={}, failed={}",
        initialImport, leagueIds, dayOffsets, daySavedCount, daySkippedCount,
        requestedCount, succeededCount, failedCount);

    failIfAllRequestsFailed(requestedCount, succeededCount);

    return RepeatStatus.FINISHED;
  }

  private List<Integer> getDayOffsets(boolean initialImport) {
    int start = initialImport
        ? properties.getInitialSportsDbDayOffsetStart()
        : properties.getSportsDbDayOffsetStart();
    int end = initialImport
        ? properties.getInitialSportsDbDayOffsetEnd()
        : properties.getSportsDbDayOffsetEnd();

    return IntStream.rangeClosed(start, end)
        .boxed()
        .toList();
  }

  private boolean isInitialImport(ChunkContext chunkContext) {
    if (chunkContext == null) {
      return false;
    }

    StepContext stepContext = chunkContext.getStepContext();
    if (stepContext == null) {
      return false;
    }

    Object importMode = stepContext.getJobParameters().get(IMPORT_MODE_PARAMETER);
    return IMPORT_MODE_INITIAL.equals(importMode);
  }

  private void delayNextRequest() {
    long delayMillis = properties.getSportsDbRequestDelayMillis();
    if (delayMillis <= 0) {
      return;
    }

    try {
      Thread.sleep(delayMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("The Sports DB 요청 대기 중 인터럽트가 발생했습니다.", e);
    }
  }

  private void failIfAllRequestsFailed(int requestedCount, int succeededCount) {
    if (requestedCount > 0 && succeededCount == 0) {
      throw new IllegalStateException("The Sports DB 콘텐츠 수집 요청이 모두 실패했습니다.");
    }
  }
}
