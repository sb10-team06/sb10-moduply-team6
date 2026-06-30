package com.team6.moduply.directmessage.repository;

import com.team6.moduply.directmessage.entity.DirectMessage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

  /// 특정 대화방에 해당하는 DM중 생성일자를 내림차순으로 정렬했을때 가낭 위에꺼: 가장 최신 DM
  Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

  /// 대화방에서 senderId에 해당하는 사람이 상대방 메시지를 읽지않은게 있는지?
  /// senderId가 A라면: B가 보낸 읽지않은 메시지가 있는가?
  boolean existsByConversationIdAndSenderIdNotAndReadFalse(UUID conversationId, UUID senderId);
}
