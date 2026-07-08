package com.team6.moduply.auth.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;
import java.util.UUID;

public class AuthException extends ModuPlyException {

  public AuthException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public AuthException(ErrorCode errorCode, UUID id) {
    super(errorCode, id);
  }
}
