package com.team6.moduply.common.config;

import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

  public static final String TEMP_PASSWORD_MAIL_TASK_EXECUTOR = "tempPasswordMailTaskExecutor";
  public static final String WATCHING_EVENT_TASK_EXECUTOR = "watchingEventTaskExecutor";
  public static final String NOTIFICATION_TASK_EXECUTOR = "notificationTaskExecutor";
  public static final String BINARY_CONTENT_TASK_EXECUTOR = "binaryContentTaskExecutor";
  public static final String REDIS_MESSAGE_TASK_EXECUTOR = "redisMessageTaskExecutor";

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

  @Bean(name = WATCHING_EVENT_TASK_EXECUTOR)
  public TaskExecutor watchingEventTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 이용 가능한 프로세서 수
    int processors = Runtime.getRuntime().availableProcessors();
    int coreSize = Math.max(3, processors / 2);

    executor.setCorePoolSize(coreSize);
    executor.setMaxPoolSize(Math.max(processors, coreSize));
    executor.setQueueCapacity(5000);
    executor.setThreadNamePrefix("watching-event-task-");
    executor.setWaitForTasksToCompleteOnShutdown(true); // 진행 중인 작업 완료 후 종료
    executor.setAwaitTerminationSeconds(100);           // 최대 대기 시간 설정
    //유실허용
    executor.setRejectedExecutionHandler((r, e) -> {
      log.warn("시청 이벤트 브로드캐스트 작업이 거부되었습니다. poolSize={}, activeCount={}, queueSize={}",
          e.getPoolSize(), e.getActiveCount(), e.getQueue().size());
    });
    executor.initialize();
    return executor;
  }

  @Bean(name = REDIS_MESSAGE_TASK_EXECUTOR)
  public TaskExecutor redisTaskExecutor() {
    int processors = Runtime.getRuntime().availableProcessors();
    int coreSize = Math.max(10, processors / 2);

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(coreSize);
    executor.setMaxPoolSize(Math.max(processors, coreSize));
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("redis-message-task-");
    executor.setWaitForTasksToCompleteOnShutdown(true); // 진행 중인 작업 완료 후 종료
    executor.setAwaitTerminationSeconds(100);           // 최대 대기 시간 설정
    executor.setRejectedExecutionHandler((r, e) -> {
      log.warn("Redis Message 작업이 거부되었습니다. poolSize={}, activeCount={}, queueSize={}",
          e.getPoolSize(), e.getActiveCount(), e.getQueue().size());
    });
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) -> {
      log.error("비동기 처리 중 예외 발생 - 메서드: {}, 파라미터: {}",
          method.getName(),
          extractParamTypes(params),
          ex);
      //추후 알림 추가 가능
    };
  }

  private String extractParamTypes(Object[] params) {
    if (params == null || params.length == 0) {
      return "[]";
    }
    return Arrays.stream(params)
        .map(p -> p == null ? "null" : p.getClass().getSimpleName())
        .toList()
        .toString();
  }

  // TODO: @Async 스레드에서도 MDC(requestId 등)와 로그인 사용자 정보가 유지되도록
  //  TaskDecorator를 추가해 부모 스레드의 컨텍스트를 복사/정리하는 설정 필요

  //TODO: 스레드 풀 상태를 Actuator/Micrometer로 모니터링하는 설정 필요

  @Bean(NOTIFICATION_TASK_EXECUTOR)
  public TaskExecutor notificationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("notification-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setRejectedExecutionHandler((r, e) -> {
      log.warn("알림 발송 작업이 거부되었습니다. poolSize={}, activeCount={}, queueSize={}",
          e.getPoolSize(), e.getActiveCount(), e.getQueue().size());
    });
    executor.initialize();
    return executor;
  }

  @Bean(BINARY_CONTENT_TASK_EXECUTOR)
  public TaskExecutor binaryContentTaskExecutor(
      @Value("${moduply.async.binary-content.enabled:true}") boolean asyncEnabled
  ) {
    if (!asyncEnabled) {
      return new SyncTaskExecutor();
    }

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("binary-content-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
