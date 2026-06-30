package com.team6.moduply.watching.event;

import com.team6.moduply.watching.enums.ChangeType;
import com.team6.moduply.watching.model.WatchingSession;

public record WatchingSessionChangedEvent(
    ChangeType type,
    WatchingSession watchingSession

) {

}
