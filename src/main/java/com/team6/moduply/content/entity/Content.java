package com.team6.moduply.content.entity;

import com.team6.moduply.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "contents",
    indexes = {
        @Index(name = "idx_contents_title", columnList = "title"),
        @Index(name = "idx_contents_type", columnList = "type")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseEntity {

  @Column(name = "content_img_id")
  private UUID contentImgId;

  @Column(nullable = false, length = 30)
  private String type;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "thumbnail_url", length = 500)
  private String thumbnailUrl;

  @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal averageRating = BigDecimal.ZERO;

  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0;

  @Column(name = "watcher_count", nullable = false)
  private long watcherCount = 0L;

  public Content(UUID contentImgId, String type, String title, String description, String thumbnailUrl) {
    this.contentImgId = contentImgId;
    this.type = type;
    this.title = title;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
  }
}
