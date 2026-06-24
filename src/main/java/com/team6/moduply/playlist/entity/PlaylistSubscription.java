package com.team6.moduply.playlist.entity;

import com.team6.moduply.common.baseentity.BaseEntity;
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
    name = "playlist_subscriptions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "subscriber_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSubscription extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @Column(nullable = false)
  private UUID subscriberId;

  @Builder
  public PlaylistSubscription(Playlist playlist, UUID subscriberId) {
    this.playlist = playlist;
    this.subscriberId = subscriberId;
  }

}
