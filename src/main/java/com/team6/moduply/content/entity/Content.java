package com.team6.moduply.content.entity;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import com.team6.moduply.content.enums.ContentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "contents",
    indexes = {
        @Index(name = "idx_contents_title", columnList = "title"),
        @Index(name = "idx_contents_type", columnList = "type"),
        @Index(name = "idx_contents_created_at_id", columnList = "created_at, id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseUpdatableEntity {

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "content_img_id")
  private BinaryContent contentImg;

  @Column(name = "external_api_id", length = 100)
  private String externalApiId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ContentType type;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
  private BigDecimal averageRating = BigDecimal.ZERO;

  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0;

  @Column(name = "watcher_count", nullable = false)
  private long watcherCount = 0L;

  public Content(
      BinaryContent contentImg,
      String externalApiId,
      ContentType type,
      String title,
      String description
  ) {
    this.contentImg = contentImg;
    this.externalApiId = externalApiId;
    this.type = type;
    this.title = title;
    this.description = description;
  }

  public void updateContentImg(BinaryContent contentImg) {
    this.contentImg = contentImg;
  }

  public void update(
      String title,
      String description
  ) {
    if (title != null) {
      this.title = title;
    }

    if (description != null) {
      this.description = description;
    }
  }
}
