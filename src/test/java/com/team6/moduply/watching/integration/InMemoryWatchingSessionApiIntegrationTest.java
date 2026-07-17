package com.team6.moduply.watching.integration;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "watching.storage.type=in-memory")
public class InMemoryWatchingSessionApiIntegrationTest extends WatchingSessionApiIntegrationTest {

}
