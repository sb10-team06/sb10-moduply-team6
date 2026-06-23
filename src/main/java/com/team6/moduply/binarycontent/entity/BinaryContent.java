package com.team6.moduply.binarycontent.entity;

import com.team6.moduply.binarycontent.BinaryContentStatus;
import com.team6.moduply.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "binary_contents")
public class BinaryContent extends BaseEntity {

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
}
