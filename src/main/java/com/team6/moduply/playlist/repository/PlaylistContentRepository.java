package com.team6.moduply.playlist.repository;

import com.team6.moduply.playlist.entity.PlaylistContent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

}
