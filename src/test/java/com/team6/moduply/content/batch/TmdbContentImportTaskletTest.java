package com.team6.moduply.content.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.external.dto.ExternalContentImportResult;
import com.team6.moduply.content.external.service.ExternalContentImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class TmdbContentImportTaskletTest {

  @Mock
  private ExternalContentImportService externalContentImportService;

  @Mock
  private ContentImportBatchProperties properties;

  @InjectMocks
  private TmdbContentImportTasklet tasklet;

  @Test
  @DisplayName("TMDB 콘텐츠 수집 Tasklet 실행 시 영화와 TV 콘텐츠를 수집한다.")
  void execute_success_import_tmdb_contents() throws Exception {
    // Given
    ExternalContentImportResult result = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(properties.getTmdbPageStart()).willReturn(1);
    given(properties.getTmdbPageEnd()).willReturn(3);
    given(properties.getTmdbLanguage()).willReturn("ko-KR");
    given(externalContentImportService.importTmdbMovies(1, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbTvSeries(1, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbMovies(2, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbTvSeries(2, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbMovies(3, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbTvSeries(3, "ko-KR")).willReturn(result);

    // When
    RepeatStatus status = tasklet.execute(null, null);

    // Then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(externalContentImportService).importTmdbMovies(1, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(1, "ko-KR");
    verify(externalContentImportService).importTmdbMovies(2, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(2, "ko-KR");
    verify(externalContentImportService).importTmdbMovies(3, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(3, "ko-KR");
  }

  @Test
  @DisplayName("초기 수집 모드이면 초기 수집용 TMDB 페이지 범위를 사용한다.")
  void execute_success_import_tmdb_contents_with_initial_page_range() throws Exception {
    // Given
    ExternalContentImportResult result = new ExternalContentImportResult(1, 1, 0, 1, 0);
    ChunkContext chunkContext = initialImportChunkContext();
    given(properties.getInitialTmdbPageStart()).willReturn(1);
    given(properties.getInitialTmdbPageEnd()).willReturn(2);
    given(properties.getTmdbLanguage()).willReturn("ko-KR");
    given(externalContentImportService.importTmdbMovies(1, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbTvSeries(1, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbMovies(2, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbTvSeries(2, "ko-KR")).willReturn(result);

    // When
    RepeatStatus status = tasklet.execute(null, chunkContext);

    // Then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(externalContentImportService).importTmdbMovies(1, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(1, "ko-KR");
    verify(externalContentImportService).importTmdbMovies(2, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(2, "ko-KR");
  }

  @Test
  @DisplayName("TMDB 콘텐츠 수집 중 일부 페이지가 실패해도 다음 페이지 수집을 계속한다.")
  void execute_success_continue_when_tmdb_page_fails() throws Exception {
    // Given
    ExternalContentImportResult result = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(properties.getTmdbPageStart()).willReturn(1);
    given(properties.getTmdbPageEnd()).willReturn(2);
    given(properties.getTmdbLanguage()).willReturn("ko-KR");
    willThrow(new RuntimeException("tmdb movie failed"))
        .given(externalContentImportService)
        .importTmdbMovies(1, "ko-KR");
    given(externalContentImportService.importTmdbTvSeries(1, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbMovies(2, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbTvSeries(2, "ko-KR")).willReturn(result);

    // When
    RepeatStatus status = tasklet.execute(null, null);

    // Then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(externalContentImportService).importTmdbMovies(1, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(1, "ko-KR");
    verify(externalContentImportService).importTmdbMovies(2, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(2, "ko-KR");
  }

  @Test
  @DisplayName("TMDB TV 수집이 실패해도 다음 페이지 수집을 계속한다.")
  void execute_success_continue_when_tmdb_tv_fails() throws Exception {
    // Given
    ExternalContentImportResult result = new ExternalContentImportResult(1, 1, 0, 1, 0);
    given(properties.getTmdbPageStart()).willReturn(1);
    given(properties.getTmdbPageEnd()).willReturn(2);
    given(properties.getTmdbLanguage()).willReturn("ko-KR");
    given(externalContentImportService.importTmdbMovies(1, "ko-KR")).willReturn(result);
    willThrow(new RuntimeException("tmdb tv failed"))
        .given(externalContentImportService)
        .importTmdbTvSeries(1, "ko-KR");
    given(externalContentImportService.importTmdbMovies(2, "ko-KR")).willReturn(result);
    given(externalContentImportService.importTmdbTvSeries(2, "ko-KR")).willReturn(result);

    // When
    RepeatStatus status = tasklet.execute(null, null);

    // Then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(externalContentImportService).importTmdbMovies(1, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(1, "ko-KR");
    verify(externalContentImportService).importTmdbMovies(2, "ko-KR");
    verify(externalContentImportService).importTmdbTvSeries(2, "ko-KR");
  }

  private ChunkContext initialImportChunkContext() {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("importMode", "INITIAL")
        .toJobParameters();
    JobInstance jobInstance = new JobInstance(1L, "externalContentImportJob");
    JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
    StepExecution stepExecution = new StepExecution("tmdbContentImportStep", jobExecution);
    return new ChunkContext(new StepContext(stepExecution));
  }
}
