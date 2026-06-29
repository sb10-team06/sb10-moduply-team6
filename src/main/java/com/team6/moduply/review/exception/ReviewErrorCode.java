package com.team6.moduply.review.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

  REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "review01", "리뷰를 찾을 수 없습니다."),
  REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "review02", "리뷰에 대한 권한이 없습니다."),
  REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "review03", "이미 해당 콘텐츠에 리뷰를 작성했습니다."),
  CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "review04", "콘텐츠를 찾을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
