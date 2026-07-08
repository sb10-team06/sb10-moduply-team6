package com.team6.moduply.binarycontent.s3.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;

import java.util.Map;

public class S3StorageException extends ModuPlyException {
    public S3StorageException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }

    public S3StorageException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        super(errorCode, details, cause);
    }

}
