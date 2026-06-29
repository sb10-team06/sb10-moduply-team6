package com.team6.moduply.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
  public static final String TEMP_PASSWORD_MAIL_TASK_EXECUTOR = "tempPasswordMailTaskExecutor";

  @Bean(TEMP_PASSWORD_MAIL_TASK_EXECUTOR)
  public TaskExecutor tempPasswordMailTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("temp-password-mail-");
    executor.initialize();
    return executor;
  }

  //TODO: 비동기 스레드에서도 requestId와 로그인 사용자 정보를 유지 설정 필요

  //TODO: 스레드 풀 상태를 Actuator/Micrometer로 모니터링하는 설정 필요
}
