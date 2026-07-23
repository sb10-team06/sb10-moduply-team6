package com.team6.moduply.common.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(
    name = "moduply.async.event.type",
    havingValue = "kafka"
)
public class KafkaProduceRequiredEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(
      phase = TransactionPhase.AFTER_COMMIT,
      fallbackExecution = true // 트랜잭션이 없어도 실행되도록 설정(시청세션같은 트랜잭션 없는 이벤트를 위해)
  )
  public void on(ModuplyAsyncEvent event) {
    log.info("ModuplyAsyncEvent 수신 하여 Kafka 발행");
    try {
      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload);
    } catch (JsonProcessingException e) {
      log.error("ModuplyAsyncEvent 직렬화 실패", e);
      throw new RuntimeException(e);
    }
  }
}