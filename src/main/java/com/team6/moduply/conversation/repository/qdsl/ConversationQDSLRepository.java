package com.team6.moduply.conversation.repository.qdsl;

import com.team6.moduply.conversation.dto.ConversationFindAllRequest;
import com.team6.moduply.conversation.entity.Conversation;
import java.util.List;
import java.util.UUID;

public interface ConversationQDSLRepository {

  List<Conversation> findAllWithCursor(ConversationFindAllRequest request, UUID currentUserId);

  long countWithCondition(ConversationFindAllRequest request, UUID currentUserId);
}
