package com.team6.moduply.content.entity;

import com.team6.moduply.common.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "tags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tags_name", columnNames = "tag_name")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

  @Column(name = "tag_name", nullable = false, length = 100)
  private String tagName;

  public Tag(String tagName) {
    this.tagName = tagName;
  }
}
