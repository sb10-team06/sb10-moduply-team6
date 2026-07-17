package com.team6.moduply.watching.integration;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "watching.storage.type=in-memory"
)
public class InMemoryContentChatIntegrationTest extends ContentChatIntegrationTest {

}
