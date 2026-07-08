package com.team6.moduply.watching.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;
import java.util.UUID;

public class WatchingSessionException extends ModuPlyException {

  public WatchingSessionException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public WatchingSessionException(ErrorCode errorCode, Map<String, Object> details,
      Throwable cause) {
    super(errorCode, details, cause);
  }

  public WatchingSessionException(ErrorCode errorCode, UUID id) {
    super(errorCode, id);
  }
}
