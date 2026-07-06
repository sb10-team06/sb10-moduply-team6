package com.team6.moduply.content.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;
import java.util.UUID;

public class ContentException extends ModuPlyException {

  public ContentException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public ContentException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }

  public ContentException(ErrorCode errorCode, UUID id) {
    super(errorCode, id);
  }
}
