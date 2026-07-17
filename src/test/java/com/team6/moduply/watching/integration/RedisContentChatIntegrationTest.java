package com.team6.moduply.watching.integration;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "watching.storage.type=redis")
public class RedisContentChatIntegrationTest extends ContentChatIntegrationTest {

}
