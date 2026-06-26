package com.team6.moduply.common.websocket.events;

public record StompUnSubscribeEvent(
    String sessionId,
    String destination
) {

}
