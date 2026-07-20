package com.team6.moduply.conversation.repository;

import com.team6.moduply.conversation.entity.ConversationUserState;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationUserStateRepository extends JpaRepository<ConversationUserState, UUID> {

  List<ConversationUserState> findAllByConversationIdAndUserIdIn(
      UUID conversationId,
      Collection<UUID> userIds
  );

  Optional<ConversationUserState> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}
