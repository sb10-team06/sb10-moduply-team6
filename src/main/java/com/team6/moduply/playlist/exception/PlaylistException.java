package com.team6.moduply.playlist.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;
import java.util.UUID;

public class PlaylistException extends ModuPlyException {

  public PlaylistException(ErrorCode errorCode, UUID id) {
    super(errorCode, id);
  }
}
