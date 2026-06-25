package com.team6.moduply.playlist.exception;

import com.team6.moduply.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {

  PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "playlist01", "플레이리스트를 찾을 수 없습니다."),
  PLAYLIST_FORBIDDEN(HttpStatus.FORBIDDEN, "playlist02", "플레이리스트에 대한 권한이 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
  
  @Override
  public String getCode() {
    return code;
  }

}
