package com.team6.moduply.content.batch;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "moduply.content.batch.import.enabled",
    havingValue = "true"
)
public class ContentImportScheduler {

  private final JobLauncher jobLauncher;
  private final Job externalContentImportJob;

  @Scheduled(cron = "${moduply.content.batch.import.cron}")
  public void runExternalContentImportJob() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("requestedAt", Instant.now().toString())
        .toJobParameters();

    log.info("외부 콘텐츠 수집 배치 실행 시작: jobName={}", externalContentImportJob.getName());
    jobLauncher.run(externalContentImportJob, jobParameters);
    log.info("외부 콘텐츠 수집 배치 실행 요청 완료: jobName={}", externalContentImportJob.getName());
  }
}
