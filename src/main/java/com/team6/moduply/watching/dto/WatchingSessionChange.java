package com.team6.moduply.watching.dto;

import com.team6.moduply.watching.enums.ChangeType;

public record WatchingSessionChange(

    ChangeType type,

    WatchingSessionDto watchingSession,

    long watcherCount
) {

}
