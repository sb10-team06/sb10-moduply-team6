package com.team6.moduply.playlist.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;
import java.util.UUID;

public class PlaylistException extends ModuPlyException {

  public PlaylistException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public PlaylistException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}