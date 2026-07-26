package com.team6.moduply.config;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
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

  private static final KafkaContainer kafka = new KafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
      .withStartupTimeout(Duration.ofSeconds(120));

  static {
    postgres.start();
    redis.start();
    kafka.start();
  }

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return postgres;
  }


  /**
   * Redis 컨테이너를 스프링 레디스 커넥션 팩토리에 자동 연결합니다.
   * <p>호스트 PC 환경과의 포트 충돌을 방지하기 위해 도커가 할당한 랜덤 포트를
   * {@code spring.data.redis.port}에 동적으로 바인딩합니다.</p>
   */
  @Bean
  @ServiceConnection(name = "redis")
  public GenericContainer<?> redisContainer() {
    return redis;
  }

  @Bean
  @ServiceConnection
  public KafkaContainer kafkaContainer() {
    return kafka;
  }
}
