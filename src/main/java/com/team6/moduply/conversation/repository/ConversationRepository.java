package com.team6.moduply.conversation.repository;

import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.repository.qdsl.ConversationQDSLRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID>,
    ConversationQDSLRepository {

  /// 대화방 조회
  Optional<Conversation> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);
}
