package com.team6.moduply.common.kafka;

public interface ModuplyAsyncEvent {

  String getTopic();        // 카프카 토픽명 (예: "watching-session-events")

  String getPartitionKey(); // 순서 보장을 위한 키 (예: userId 또는 contentId)
}