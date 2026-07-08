package com.team6.moduply.directmessage.repository;

import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.repository.qdsl.DirectMessageQDSLRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>,
    DirectMessageQDSLRepository {

  /// 특정 대화방에 해당하는 DM중 생성일자를 내림차순으로 정렬했을때 가낭 위에꺼: 가장 최신 DM
  /// // TODO: 최신 DM을 조회하는 과정에서 CreatedAt이 같을경우를 생각.
  Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

  /// 대화방에서 senderId에 해당하는 사람이 상대방 메시지를 읽지않은게 있는지?
  /// senderId가 A라면: B가 보낸 읽지않은 메시지가 있는가?
  boolean existsByConversationIdAndSenderIdNotAndReadFalse(UUID conversationId, UUID senderId);

  Optional<DirectMessage> findByIdAndConversationId(UUID id, UUID conversationId);

  // 수정
  // 현재 로그인한 사용자가 보낸 메시지는 읽음처리 x(상대방이 보낸 메시지만 읽음처리)
  // TODO: 복합 인덱스 고려
  @Modifying(clearAutomatically = true)
  @Query("""
    update DirectMessage dm
    set dm.read = true
    where dm.conversation.id = :conversationId
      and dm.sender.id <> :currentUserId
      and dm.read = false
      and (
        dm.createdAt < :readUntilCreatedAt
        or (
          dm.createdAt = :readUntilCreatedAt
          and dm.id <= :readUntilMessageId
        )
      )
    """)
  int markUnreadMessagesAsReadUntil(
          @Param("conversationId") UUID conversationId,
          @Param("currentUserId") UUID currentUserId,
          @Param("readUntilCreatedAt") Instant readUntilCreatedAt,
          @Param("readUntilMessageId") UUID readUntilMessageId
  );

  @Query("""
      select distinct dm.conversation.id
      from DirectMessage dm
      where dm.conversation.id in :conversationIds
        and dm.sender.id <> :currentUserId
        and dm.read = false
      """)
  List<UUID> findUnreadConversationIds(
      @Param("conversationIds") Collection<UUID> conversationIds,
      @Param("currentUserId") UUID currentUserId
  );
}
