package com.team6.moduply.conversation.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;

public class ConversationException extends ModuPlyException {

  public ConversationException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public ConversationException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
