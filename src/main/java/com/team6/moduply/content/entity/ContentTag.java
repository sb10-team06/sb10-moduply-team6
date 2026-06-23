package com.team6.moduply.content.entity;

import com.team6.moduply.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "content_tags",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_content_tags_content_tag",
            columnNames = {"content_id", "tag_id"}
        )
    },
    indexes = {
        @Index(name = "idx_content_tags_tag_id", columnList = "tag_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTag extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;

  public ContentTag(Content content, Tag tag) {
    this.content = content;
    this.tag = tag;
  }
}
