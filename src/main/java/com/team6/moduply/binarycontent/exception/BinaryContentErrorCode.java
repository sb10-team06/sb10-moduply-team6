package com.team6.moduply.binarycontent.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BinaryContentErrorCode implements ErrorCode {
  EMPTY_FILE(HttpStatus.BAD_REQUEST, "binaryContent01", "파일은 비어 있을 수 없습니다."),
  INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "binaryContent02", "파일 크기는 0보다 커야 합니다."),
  UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "binaryContent03", "지원하지 않는 이미지 형식입니다."),
  FILE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "binaryContent04", "파일을 읽는 중 오류가 발생했습니다."),
  BINARY_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "binaryContent05", "존재하지 않는 파일입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
