package com.team6.moduply.binarycontent.entity;

import com.team6.moduply.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "binary_contents")
public class BinaryContent extends BaseEntity {

  // 상세 필드는 추후 반영
}
