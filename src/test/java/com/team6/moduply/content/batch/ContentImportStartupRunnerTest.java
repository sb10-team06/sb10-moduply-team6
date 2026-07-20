package com.team6.moduply.content.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.content.repository.ContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class ContentImportStartupRunnerTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job tmdbContentImportJob;

  @Mock
  private Job sportsDbContentImportJob;

  @Mock
  private ContentImportBatchProperties properties;

  @Mock
  private ContentRepository contentRepository;

  @Test
  @DisplayName("콘텐츠가 이미 존재하면 초기 수집 배치를 실행하지 않는다.")
  void run_skip_when_content_exists() throws Exception {
    // Given
    given(contentRepository.count()).willReturn(1L);

    // When
    runner().run();

    // Then
    verify(jobLauncher, never()).run(eq(tmdbContentImportJob), org.mockito.Mockito.any());
    verify(jobLauncher, never()).run(eq(sportsDbContentImportJob), org.mockito.Mockito.any());
  }

  @Test
  @DisplayName("콘텐츠가 비어 있으면 TMDB와 Sports DB 초기 수집 배치를 실행한다.")
  void run_success_when_content_empty() throws Exception {
    // Given
    given(contentRepository.count()).willReturn(0L, 10L);
    given(tmdbContentImportJob.getName()).willReturn("tmdbContentImportJob");
    given(sportsDbContentImportJob.getName()).willReturn("sportsDbContentImportJob");
    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);

    // When
    runner().run();

    // Then
    verify(jobLauncher).run(eq(tmdbContentImportJob), captor.capture());
    verify(jobLauncher).run(eq(sportsDbContentImportJob), captor.capture());
    assertThat(captor.getAllValues()).allSatisfy(jobParameters -> {
      assertThat(jobParameters.getString("importMode")).isEqualTo("INITIAL");
      assertThat(jobParameters.getString("initialRunKey")).isEqualTo("default");
    });
  }

  private ContentImportStartupRunner runner() {
    return new ContentImportStartupRunner(
        jobLauncher,
        tmdbContentImportJob,
        sportsDbContentImportJob,
        properties,
        contentRepository
    );
  }
}
