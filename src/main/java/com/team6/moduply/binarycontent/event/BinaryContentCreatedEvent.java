package com.team6.moduply.binarycontent.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class BinaryContentCreatedEvent {
    UUID binaryContentId;
    byte[] bytes;
    UUID userId;
    UUID contentId;
}
