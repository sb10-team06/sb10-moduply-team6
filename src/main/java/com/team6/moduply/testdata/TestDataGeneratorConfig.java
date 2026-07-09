package com.team6.moduply.testdata;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Profile("data-gen")
public class TestDataGeneratorConfig {

  @Bean
  public Executor dataGeneratorExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();                   // ThreadPoolTaskExecutor: 스레드 풀 관리 클래스
    executor.setCorePoolSize(4);                                                      // 기본적으로 유지할 스레드 개수(4개)
    executor.setMaxPoolSize(8);                                                       // 최대 생성가능한 스레드 개수(8개)
    executor.setQueueCapacity(100);                                                   // 작업 대기열 크기(100)
    executor.setThreadNamePrefix("data-generator-");                                  // 생성되는 스레드 이름의 접두사
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());  // 스레드 풀과 큐 모두 찼을때: 거부정책
    executor.initialize();
    return executor;
  }
}
