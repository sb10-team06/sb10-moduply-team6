package com.team6.moduply.directmessage.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;

public class DirectMessageException extends ModuPlyException {

  public DirectMessageException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public DirectMessageException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
