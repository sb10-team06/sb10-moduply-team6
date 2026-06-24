package com.team6.moduply.playlist.repository;

import com.team6.moduply.playlist.entity.PlaylistSubscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

}
