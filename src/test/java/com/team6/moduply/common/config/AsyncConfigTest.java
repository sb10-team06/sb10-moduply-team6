package com.team6.moduply.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

  private final AsyncConfig asyncConfig = new AsyncConfig();

  @Test
  @DisplayName("임시 비밀번호 메일 발송 전용 TaskExecutor를 생성한다")
  void temp_password_mail_task_executor_success() {
    // When
    TaskExecutor taskExecutor = asyncConfig.tempPasswordMailTaskExecutor();

    // Then
    assertThat(taskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);

    ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
    assertThat(executor.getCorePoolSize()).isEqualTo(2);
    assertThat(executor.getMaxPoolSize()).isEqualTo(4);
    assertThat(executor.getQueueCapacity()).isEqualTo(100);
    assertThat(executor.getThreadNamePrefix()).isEqualTo("temp-password-mail-");

    executor.shutdown();
  }

  @Test
  @DisplayName("사용자 보안 이벤트 전용 TaskExecutor를 생성한다")
  void user_security_task_executor_success() {
    // When
    TaskExecutor taskExecutor = asyncConfig.userSecurityTaskExecutor();

    // Then
    assertThat(taskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);

    ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
    assertThat(executor.getCorePoolSize()).isEqualTo(2);
    assertThat(executor.getMaxPoolSize()).isEqualTo(4);
    assertThat(executor.getQueueCapacity()).isEqualTo(100);
    assertThat(executor.getThreadNamePrefix()).isEqualTo("user-security-");

    executor.shutdown();
  }
}
