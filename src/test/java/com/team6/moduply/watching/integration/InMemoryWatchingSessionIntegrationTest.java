package com.team6.moduply.watching.integration;

import org.springframework.test.context.TestPropertySource;

//인메모리 저장소 및 기본 스프링 이벤트를 활용한 시청 세션 관련 로직을 검증합니다.
@TestPropertySource(properties = {
    "watching.storage.type=in-memory",
    "moduply.async.event.type=spring",
    "spring.kafka.listener.auto-startup=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
public class InMemoryWatchingSessionIntegrationTest extends WatchingSessionIntegrationTest {

}
