package com.team6.moduply.config;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

  private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
      "postgres:15-alpine")
      .withStartupTimeout(Duration.ofSeconds(120));

  private static final GenericContainer<?> redis = new GenericContainer<>(
      DockerImageName.parse("redis:7.2-alpine"))
      .withExposedPorts(6379)
      .withStartupTimeout(Duration.ofSeconds(120));

  static {
    postgres.start();
    redis.start();
  }

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return postgres;
  }

  // 컨테이너에서 띄 운 Redis가 실제 내 pc포트에 접근할때는 랜덤포트로 접근하게 되는데 내가 application.yml에 설정한 포트와
  // 맞지 않아서 테스트가 실패하게 된다. 그래서 DynamicPropertySource를 이용해서 실제 컨테이너에서 띄운 Redis의 포트를 가져와서
  // application.yml의 spring.data.redis.port에 동적으로 등록해줌
  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }
}
