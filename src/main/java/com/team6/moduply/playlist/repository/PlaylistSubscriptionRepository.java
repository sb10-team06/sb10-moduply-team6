package com.team6.moduply.playlist.repository;

import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.entity.PlaylistSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

  boolean existsByPlaylistAndSubscriberId(Playlist playlist, UUID subscriberId);

  Optional<PlaylistSubscription> findByPlaylistAndSubscriberId(Playlist playlist, UUID subscriberId);

  List<PlaylistSubscription> findAllByPlaylist(Playlist playlist);

}
