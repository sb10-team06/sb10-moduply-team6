package domain.playlist.entity;

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
@Table(name = "playlist_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSubscription extends BaseEntity {

  @Column(nullable = false)
  private UUID playlistId;

  @Column(nullable = false)
  private UUID subscriberId;

  @Builder
  public PlaylistSubscription(UUID playlistId, UUID subscriberId) {
    this.playlistId = playlistId;
    this.subscriberId = subscriberId;
  }

}
