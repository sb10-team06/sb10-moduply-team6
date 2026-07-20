package com.team6.moduply.conversation.entity;

import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Entity
@Getter
@Table(
    name = "conversation_user_states",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_conversation_user_states_conversation_user",
            columnNames = {"conversation_id", "user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_conversation_user_states_user_conversation", columnList = "user_id, conversation_id")
    }
)
public class ConversationUserState extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "last_read_message_id")
  private UUID lastReadMessageId;

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @Column(name = "unread_count", nullable = false)
  private long unreadCount = 0L;

  protected ConversationUserState() {
  }

  private ConversationUserState(Conversation conversation, UUID userId) {
    this.conversation = Objects.requireNonNull(conversation, "대화방은 null일 수 없습니다.");
    this.userId = Objects.requireNonNull(userId, "사용자 ID는 null일 수 없습니다.");
  }

  public static ConversationUserState create(Conversation conversation, UUID userId) {
    return new ConversationUserState(conversation, userId);
  }

  public void increaseUnreadCount() {
    this.unreadCount++;
  }

  public void decreaseUnreadCount(long count) {
    if (count <= 0L) {
      return;
    }
    this.unreadCount = Math.max(0L, this.unreadCount - count);
  }

  public void markAsRead(UUID messageId, Instant readAt) {
    this.lastReadMessageId = Objects.requireNonNull(messageId, "메시지 ID는 null일 수 없습니다.");
    this.lastReadAt = Objects.requireNonNull(readAt, "읽음 처리 시각은 null일 수 없습니다.");
  }

  public boolean hasUnread() {
    return unreadCount > 0L;
  }
}
