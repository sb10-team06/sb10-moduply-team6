package com.team6.moduply.binarycontent.s3.exception;

import com.team6.moduply.common.error.ErrorCode;
import com.team6.moduply.common.error.ModuPlyException;

import java.util.Map;
import java.util.UUID;

public class S3UploadException extends ModuPlyException {
    public S3UploadException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
