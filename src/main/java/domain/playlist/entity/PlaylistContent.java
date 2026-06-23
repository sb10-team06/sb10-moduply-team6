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
@Table(name = "playlist_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistContent extends BaseEntity {

  @Column(nullable = false)
  private UUID playlistId;

  @Column(nullable = false)
  private UUID contentId;

  @Builder
  public PlaylistContent(UUID playlistId, UUID contentId) {
    this.playlistId = playlistId;
    this.contentId = contentId;
  }

}
