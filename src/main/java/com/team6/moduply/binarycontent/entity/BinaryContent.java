package com.team6.moduply.binarycontent.entity;

import com.team6.moduply.binarycontent.BinaryContentStatus;
import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "binary_contents")
public class BinaryContent extends BaseUpdatableEntity {

  @Column(name = "file_name", nullable = false, length = 100)
  private String fileName;

  @Column(nullable = false)
  private Long size;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT", unique = true)
  private String storageKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BinaryContentStatus status;

  private BinaryContent(String fileName, Long size, String contentType, String storageKey) {
    this.fileName = fileName;
    this.size = size;
    this.contentType = contentType;
    this.storageKey = storageKey;
    this.status = BinaryContentStatus.PROCESSING;
  }

  public static BinaryContent create(String fileName, Long size, String contentType, String storageKey) {
    return new BinaryContent(fileName, size, contentType, storageKey);
  }

  public void success() {
    this.status = BinaryContentStatus.SUCCESS;
  }

  public void fail() {
    this.status = BinaryContentStatus.FAIL;
  }
}
