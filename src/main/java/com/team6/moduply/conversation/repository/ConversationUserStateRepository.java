package com.team6.moduply.conversation.repository;

import com.team6.moduply.conversation.entity.ConversationUserState;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationUserStateRepository extends JpaRepository<ConversationUserState, UUID> {

  List<ConversationUserState> findAllByConversationIdAndUserIdIn(
      UUID conversationId,
      Collection<UUID> userIds
  );

  Optional<ConversationUserState> findByConversationIdAndUserId(UUID conversationId, UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = """
          update conversation_user_states
          set
            unread_count = unread_count + 1,
            updated_at = current_timestamp
          where conversation_id = :conversationId
            and user_id = :userId
          """,
      nativeQuery = true
  )
  int increaseUnreadCount(
      @Param("conversationId") UUID conversationId,
      @Param("userId") UUID userId
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = """
          update conversation_user_states
          set
            last_read_message_id = :lastReadMessageId,
            last_read_at = :lastReadAt,
            unread_count = greatest(0, unread_count - :readCount),
            updated_at = current_timestamp
          where conversation_id = :conversationId
            and user_id = :userId
          """,
      nativeQuery = true
  )
  int markAsReadAndDecreaseUnreadCount(
      @Param("conversationId") UUID conversationId,
      @Param("userId") UUID userId,
      @Param("lastReadMessageId") UUID lastReadMessageId,
      @Param("lastReadAt") Instant lastReadAt,
      @Param("readCount") long readCount
  );
}
