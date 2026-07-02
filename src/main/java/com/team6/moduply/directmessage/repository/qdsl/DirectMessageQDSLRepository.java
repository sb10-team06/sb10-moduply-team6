package com.team6.moduply.directmessage.repository.qdsl;

import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.dto.DirectMessageFindAllRequest;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DirectMessageQDSLRepository {

  List<DirectMessage> findLatestMessagesByConversationIds(Collection<UUID> conversationIds);

  /// 특정 대화방의 DM 목록 조회
  List<DirectMessage> findAllWithCursor(DirectMessageFindAllRequest request, UUID conversationId);
  /// 특정 대화망의 DM 총 개수 조회
  long countWithCondition(UUID conversationId);
}
