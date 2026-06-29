package com.team6.moduply.review.entity;

import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"content_id", "author_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseUpdatableEntity {

  @Column(nullable = false)
  private UUID contentId;

  @Column(nullable = false)
  private UUID authorId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String text;

  @Column(nullable = false)
  private Double rating;

  @Builder
  public Review(UUID contentId, UUID authorId, String text, Double rating) {
    this.contentId = contentId;
    this.authorId = authorId;
    this.text = text;
    this.rating = rating;
  }

  public void update(String text, Double rating) {
    this.text = text;
    this.rating = rating;
  }

}
