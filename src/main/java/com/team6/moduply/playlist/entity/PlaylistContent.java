package com.team6.moduply.playlist.entity;

import com.team6.moduply.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "playlist_contents",
    uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "content_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistContent extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @Column(nullable = false)
  private UUID contentId;

  @Builder
  public PlaylistContent(Playlist playlist, UUID contentId) {
    this.playlist = playlist;
    this.contentId = contentId;
  }

}
