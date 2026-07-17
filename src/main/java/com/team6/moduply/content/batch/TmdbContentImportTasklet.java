package com.team6.moduply.content.batch;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.service.ExternalContentImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbContentImportTasklet implements Tasklet {

  private static final String IMPORT_MODE_INITIAL = "INITIAL";
  private static final String IMPORT_MODE_PARAMETER = "importMode";

  private final ExternalContentImportService externalContentImportService;
  private final ContentImportBatchProperties properties;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    boolean initialImport = isInitialImport(chunkContext);
    int pageStart = getPageStart(initialImport);
    int pageEnd = getPageEnd(initialImport, pageStart);
    int movieSavedCount = 0;
    int movieSkippedCount = 0;
    int tvSavedCount = 0;
    int tvSkippedCount = 0;

    for (int page = pageStart; page <= pageEnd; page++) {
      try {
        ExternalContentImportResult movieResult = externalContentImportService.importTmdbMovies(
            page,
            properties.getTmdbLanguage()
        );
        movieSavedCount += movieResult.savedCount();
        movieSkippedCount += movieResult.skippedCount();
      } catch (RuntimeException e) {
        log.warn("TMDB 영화 수집 실패. page={}, language={}", page, properties.getTmdbLanguage(), e);
      }

      try {
        ExternalContentImportResult tvResult = externalContentImportService.importTmdbTvSeries(
            page,
            properties.getTmdbLanguage()
        );
        tvSavedCount += tvResult.savedCount();
        tvSkippedCount += tvResult.skippedCount();
      } catch (RuntimeException e) {
        log.warn("TMDB TV 수집 실패. page={}, language={}", page, properties.getTmdbLanguage(), e);
      }
    }

    log.info("TMDB 콘텐츠 수집 배치 완료: initialImport={}, pageStart={}, pageEnd={}, movieSaved={}, "
            + "movieSkipped={}, tvSaved={}, tvSkipped={}",
        initialImport, pageStart, pageEnd, movieSavedCount, movieSkippedCount, tvSavedCount, tvSkippedCount);

    return RepeatStatus.FINISHED;
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

  private int getPageStart(boolean initialImport) {
    int pageStart = initialImport
        ? properties.getInitialTmdbPageStart()
        : properties.getTmdbPageStart();
    return Math.max(1, pageStart);
  }

  private int getPageEnd(boolean initialImport, int pageStart) {
    int pageEnd = initialImport
        ? properties.getInitialTmdbPageEnd()
        : properties.getTmdbPageEnd();
    return Math.max(pageStart, pageEnd);
  }
}
