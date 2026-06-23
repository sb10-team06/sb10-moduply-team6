package com.team6.moduply.playlist.entity;

import com.team6.moduply.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseEntity {

  @Column(nullable = false)
  private UUID ownerId;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Builder
  public Playlist(UUID ownerId, String title, String description) {
    this.ownerId = ownerId;
    this.title = title;
    this.description = description;
  }

  public void update(String title, String description) {
    this.title = title;
    this.description = description;
  }

}
