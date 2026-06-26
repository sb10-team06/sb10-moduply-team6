package com.team6.moduply.common.websocket.events;


import java.util.UUID;

public record StompSubscribeEvent(
    String sessionId,
    UUID userId,
    String destination
) {

}
