package com.team6.moduply.content.batch;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "moduply.content.batch.import.enabled",
    havingValue = "true"
)
public class ContentImportScheduler {

  private final JobLauncher jobLauncher;
  private final Job tmdbContentImportJob;
  private final Job sportsDbContentImportJob;

  public ContentImportScheduler(
      JobLauncher jobLauncher,
      @Qualifier("tmdbContentImportJob") Job tmdbContentImportJob,
      @Qualifier("sportsDbContentImportJob") Job sportsDbContentImportJob
  ) {
    this.jobLauncher = jobLauncher;
    this.tmdbContentImportJob = tmdbContentImportJob;
    this.sportsDbContentImportJob = sportsDbContentImportJob;
  }

  @Scheduled(cron = "${moduply.content.batch.import.tmdb-cron}")
  public void runTmdbContentImportJob() {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("scheduledAt", currentScheduleKey())
        .toJobParameters();

    runJob(tmdbContentImportJob, jobParameters);
  }

  @Scheduled(cron = "${moduply.content.batch.import.sports-db-cron}")
  public void runSportsDbContentImportJob() {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("scheduledAt", currentScheduleKey())
        .toJobParameters();

    runJob(sportsDbContentImportJob, jobParameters);
  }

  private void runJob(Job job, JobParameters jobParameters) {
    log.info("외부 콘텐츠 수집 배치 실행 시작: jobName={}", job.getName());
    try {
      jobLauncher.run(job, jobParameters);
      log.info("외부 콘텐츠 수집 배치 실행 요청 완료: jobName={}", job.getName());
    } catch (JobExecutionAlreadyRunningException e) {
      log.warn("외부 콘텐츠 수집 배치가 이미 실행 중입니다. jobName={}", job.getName(), e);
    } catch (JobInstanceAlreadyCompleteException e) {
      log.info("외부 콘텐츠 수집 배치가 이미 완료된 스케줄입니다. jobName={}", job.getName());
    } catch (JobRestartException | JobParametersInvalidException e) {
      log.error("외부 콘텐츠 수집 배치 실행 요청 실패. jobName={}", job.getName(), e);
    }
  }

  private String currentScheduleKey() {
    return LocalDateTime.now()
        .truncatedTo(ChronoUnit.MINUTES)
        .toString();
  }
}
