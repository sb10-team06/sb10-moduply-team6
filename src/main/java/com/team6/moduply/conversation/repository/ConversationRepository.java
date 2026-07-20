package com.team6.moduply.conversation.repository;

import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.repository.qdsl.ConversationQDSLRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID>,
    ConversationQDSLRepository {

  /// 대화방 조회
  Optional<Conversation> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

  @Query("""
      select count(c) > 0
      from Conversation c
      where c.id = :conversationId
        and (c.user1Id = :userId or c.user2Id = :userId)
      """)
  boolean existsByIdAndParticipantId(
      @Param("conversationId") UUID conversationId,
      @Param("userId") UUID userId
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = """
          update conversations
          set
            last_message_id = :messageId,
            last_message_at = :createdAt,
            last_message_content = :content,
            last_message_sender_id = :senderId,
            updated_at = current_timestamp
          where id = :conversationId
            and (
              last_message_at is null
              or last_message_at < :createdAt
              or (
                last_message_at = :createdAt
                and (
                  last_message_id is null
                  or last_message_id < :messageId
                )
              )
            )
          """,
      nativeQuery = true
  )
  int updateLastMessageIfNewer(
      @Param("conversationId") UUID conversationId,
      @Param("messageId") UUID messageId,
      @Param("createdAt") Instant createdAt,
      @Param("content") String content,
      @Param("senderId") UUID senderId
  );
}
