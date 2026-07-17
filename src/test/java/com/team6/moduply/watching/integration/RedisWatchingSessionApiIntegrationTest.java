package com.team6.moduply.watching.integration;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "watching.storage.type=redis")
public class RedisWatchingSessionApiIntegrationTest extends WatchingSessionApiIntegrationTest {

}
