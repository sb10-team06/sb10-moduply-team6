package com.team6.moduply.content.batch;

import com.team6.moduply.content.repository.ContentRepository;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "moduply.content.batch.import.initial-import-enabled",
    havingValue = "true"
)
public class ContentImportStartupRunner {

  private static final String INITIAL_RUN_KEY = "default";

  private final JobLauncher jobLauncher;
  private final Job tmdbContentImportJob;
  private final Job sportsDbContentImportJob;
  private final ContentImportBatchProperties properties;
  private final ContentRepository contentRepository;

  public ContentImportStartupRunner(
      JobLauncher jobLauncher,
      @Qualifier("tmdbContentImportJob") Job tmdbContentImportJob,
      @Qualifier("sportsDbContentImportJob") Job sportsDbContentImportJob,
      ContentImportBatchProperties properties,
      ContentRepository contentRepository
  ) {
    this.jobLauncher = jobLauncher;
    this.tmdbContentImportJob = tmdbContentImportJob;
    this.sportsDbContentImportJob = sportsDbContentImportJob;
    this.properties = properties;
    this.contentRepository = contentRepository;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(2)
  public void run() {
    long beforeCount = contentRepository.count();
    if (beforeCount > 0) {
      log.info("콘텐츠가 이미 존재하여 초기 수집 배치를 건너뜁니다. contentCount={}", beforeCount);
      return;
    }

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("importMode", "INITIAL")
        .addString("initialRunKey", INITIAL_RUN_KEY)
        .toJobParameters();

    runInitialJob(tmdbContentImportJob, jobParameters);
    runInitialJob(sportsDbContentImportJob, jobParameters);

    long afterCount = contentRepository.count();
    log.info("외부 콘텐츠 초기 수집 배치 완료: beforeCount={}, afterCount={}, increasedCount={}",
        beforeCount, afterCount, afterCount - beforeCount);
  }

  private void runInitialJob(Job job, JobParameters jobParameters) {
    log.info("외부 콘텐츠 초기 수집 배치 실행 시작: jobName={}, initialRunKey={}",
        job.getName(), jobParameters.getString("initialRunKey"));
    try {
      jobLauncher.run(job, jobParameters);
      log.info("외부 콘텐츠 초기 수집 배치 실행 요청 완료: jobName={}", job.getName());
    } catch (JobExecutionAlreadyRunningException e) {
      log.warn("외부 콘텐츠 초기 수집 배치가 이미 실행 중입니다. jobName={}", job.getName(), e);
    } catch (JobInstanceAlreadyCompleteException e) {
      log.info("외부 콘텐츠 초기 수집 배치가 이미 완료되었습니다. jobName={}, initialRunKey={}",
          job.getName(), jobParameters.getString("initialRunKey"));
    } catch (JobRestartException | JobParametersInvalidException e) {
      log.error("외부 콘텐츠 초기 수집 배치 실행 요청 실패. jobName={}", job.getName(), e);
    }
  }
}
