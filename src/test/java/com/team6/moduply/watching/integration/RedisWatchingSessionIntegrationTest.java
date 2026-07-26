package com.team6.moduply.watching.integration;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "watching.storage.type=redis")
// 기본 카프카 작동
public class RedisWatchingSessionIntegrationTest extends WatchingSessionIntegrationTest {

}
