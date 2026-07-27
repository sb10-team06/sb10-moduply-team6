package com.team6.moduply.binarycontent.dto;

import com.team6.moduply.binarycontent.entity.BinaryContent;

public record BinaryContentUploadResult(
    BinaryContent binaryContent,
    String url
) {
}
