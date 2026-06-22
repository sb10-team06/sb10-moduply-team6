package com.team6.moduply.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

  HttpStatus getHttpStatus();

  String getCode();

  String getMessage();
}
