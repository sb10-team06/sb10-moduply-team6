package com.team6.moduply.playlist.repository;

import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.entity.PlaylistSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

  long countByPlaylist(Playlist playlist);

  boolean existsByPlaylistAndSubscriberId(Playlist playlist, UUID subscriberId);

  Optional<PlaylistSubscription> findByPlaylistAndSubscriberId(Playlist playlist, UUID subscriberId);

  @Query("select ps.subscriberId from PlaylistSubscription ps where ps.playlist.id = :playlistId")
  List<UUID> findSubscriberIdsByPlaylistId(@Param("playlistId") UUID playlistId);

  @Query("select ps.playlist.id, count(ps) from PlaylistSubscription ps where ps.playlist.id in :playlistIds group by ps.playlist.id")
  List<Object[]> countByPlaylistIds(@Param("playlistIds") List<UUID> playlistIds);

  List<PlaylistSubscription> findAllBySubscriberId(UUID subscriberId);
}
