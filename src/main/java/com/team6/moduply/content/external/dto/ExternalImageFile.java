package com.team6.moduply.content.external.dto;

public record ExternalImageFile(
    String fileName,
    String contentType,
    byte[] bytes
) {

  public long size() {
    return bytes == null ? 0L : bytes.length;
  }
}
