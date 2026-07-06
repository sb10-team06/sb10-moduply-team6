package com.team6.moduply.content.batch;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.service.ExternalContentImportService;
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
public class TmdbContentImportTasklet implements Tasklet {

  private final ExternalContentImportService externalContentImportService;
  private final ContentImportBatchProperties properties;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    int pageStart = Math.max(1, properties.getTmdbPageStart());
    int pageEnd = Math.max(pageStart, properties.getTmdbPageEnd());
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

    log.info("TMDB 콘텐츠 수집 배치 완료: pageStart={}, pageEnd={}, movieSaved={}, "
            + "movieSkipped={}, tvSaved={}, tvSkipped={}",
        pageStart, pageEnd, movieSavedCount, movieSkippedCount, tvSavedCount, tvSkippedCount);

    return RepeatStatus.FINISHED;
  }
}
