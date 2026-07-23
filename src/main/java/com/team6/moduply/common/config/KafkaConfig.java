package com.team6.moduply.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
@ConditionalOnProperty(
    name = "moduply.async.event.type",
    havingValue = "kafka"
)
public class KafkaConfig {

  public static final String WATCHING_SESSION_CHANGED_TOPIC = "watching-session-changed-event";

  @Value("${moduply.kafka.topic.watching-session.partitions:3}")
  private int watchingSessionPartitions;

  @Value("${moduply.kafka.topic.watching-session.replicas:1}")
  private int watchingSessionReplicas;

  @Value("${moduply.kafka.default.partitions:3}")
  private int defaultPartitions;

  @Value("${moduply.kafka.default.replicas:1}")
  private int defaultSessionReplicas;

  @Value("${moduply.kafka.default.concurrency:3}")
  private int defaultConcurrency;

  // 애플리케이션 시작 시 토픽과 파티션 자동 생성
  @Bean
  public NewTopics customKafkaTopics() {
    return new NewTopics(
        TopicBuilder.name(WATCHING_SESSION_CHANGED_TOPIC).partitions(watchingSessionPartitions)
            .replicas(watchingSessionReplicas).build()
        //필요한 토픽 추가
    );
  }

  // Object 자동 변환
  @Bean
  public RecordMessageConverter jsonMessageConverter() {
    return new StringJsonMessageConverter();
  }

  // 스레드 개수 지정
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      ConsumerFactory<String, Object> consumerFactory,
      RecordMessageConverter messageConverter) {

    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory);
    factory.setRecordMessageConverter(messageConverter);
    // 컨슈머 스레드 풀 크기(파티션수와 동일)
    factory.setConcurrency(defaultConcurrency);

    return factory;
  }
}