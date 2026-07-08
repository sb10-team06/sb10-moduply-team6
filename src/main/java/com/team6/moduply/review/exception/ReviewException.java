package com.team6.moduply.review.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.Map;

public class ReviewException extends ModuPlyException {

  public ReviewException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
