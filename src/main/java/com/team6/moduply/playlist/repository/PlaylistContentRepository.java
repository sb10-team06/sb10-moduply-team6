package com.team6.moduply.playlist.repository;

import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.entity.PlaylistContent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

  boolean existsByPlaylistAndContentId(Playlist playlist, UUID contentId);

  Optional<PlaylistContent> findByPlaylistAndContentId(Playlist playlist, UUID contentId);

  List<PlaylistContent> findAllByPlaylist(Playlist playlist);

}
