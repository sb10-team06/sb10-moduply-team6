package com.team6.moduply.content.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ContentImportBatchProperties.class)
public class ContentImportBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TmdbContentImportTasklet tmdbContentImportTasklet;
  private final SportsDbContentImportTasklet sportsDbContentImportTasklet;

  @Bean
  public Job tmdbContentImportJob() {
    return new JobBuilder("tmdbContentImportJob", jobRepository)
        .start(tmdbContentImportStep())
        .build();
  }

  @Bean
  public Job sportsDbContentImportJob() {
    return new JobBuilder("sportsDbContentImportJob", jobRepository)
        .start(sportsDbContentImportStep())
        .build();
  }

  @Bean
  public Step tmdbContentImportStep() {
    return new StepBuilder("tmdbContentImportStep", jobRepository)
        .tasklet(tmdbContentImportTasklet, transactionManager)
        .build();
  }

  @Bean
  public Step sportsDbContentImportStep() {
    return new StepBuilder("sportsDbContentImportStep", jobRepository)
        .tasklet(sportsDbContentImportTasklet, transactionManager)
        .build();
  }
}
