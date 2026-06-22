package com.team6.moduply.common.error;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class ModuPlyException extends RuntimeException {

  private final Instant timestamp;
  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  public ModuPlyException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = details;
  }

  public ModuPlyException(ErrorCode errorCode, UUID id) {
    super(errorCode.getMessage());
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = Map.of(getClass().getSimpleName(), id);
  }
}
