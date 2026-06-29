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

  // TODO: @Async 스레드에서도 MDC(requestId 등)와 로그인 사용자 정보가 유지되도록
  //  TaskDecorator를 추가해 부모 스레드의 컨텍스트를 복사/정리하는 설정 필요

  //TODO: 스레드 풀 상태를 Actuator/Micrometer로 모니터링하는 설정 필요
}
