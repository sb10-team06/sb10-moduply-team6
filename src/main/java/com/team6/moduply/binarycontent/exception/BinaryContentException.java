package com.team6.moduply.binarycontent.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;

public class BinaryContentException extends ModuPlyException {

  public BinaryContentException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public BinaryContentException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
