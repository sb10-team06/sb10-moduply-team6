package com.team6.moduply.playlist.repository.qdsl;

import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.entity.Playlist;
import java.util.List;

public interface PlaylistQDSLRepository {

  List<Playlist> findAllWithCursor(PlaylistSearchRequest request);

  long countWithCondition(PlaylistSearchRequest request);

}
