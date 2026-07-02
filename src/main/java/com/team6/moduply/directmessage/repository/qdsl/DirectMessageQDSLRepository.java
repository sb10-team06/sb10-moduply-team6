package com.team6.moduply.directmessage.repository.qdsl;

import com.team6.moduply.directmessage.entity.DirectMessage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DirectMessageQDSLRepository {

  List<DirectMessage> findLatestMessagesByConversationIds(Collection<UUID> conversationIds);
}
