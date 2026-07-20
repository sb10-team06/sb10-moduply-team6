package com.team6.moduply.conversation.entity;

import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.Check;

@Entity
@Getter
/// 자기자신과 대화방 생성 제약
@Check(constraints = "user1_id <> user2_id")
@Table(
    name = "conversations",
    /// (user1, user2)유니크 제약조건: 대화방 중복 제거
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_conversations_user_pair",
            columnNames = {"user1_id", "user2_id"}
        )
    },
    indexes = {
        @Index(name = "idx_conversations_user1_created_at_id", columnList = "user1_id, created_at, id"),
        @Index(name = "idx_conversations_user2_created_at_id", columnList = "user2_id, created_at, id"),
        @Index(name = "idx_conversations_user1_last_message_at_id", columnList = "user1_id, last_message_at, id"),
        @Index(name = "idx_conversations_user2_last_message_at_id", columnList = "user2_id, last_message_at, id")
    }
)
public class Conversation extends BaseUpdatableEntity {

  @Column(name = "user1_id", nullable = false)
  private UUID user1Id;

  @Column(name = "user2_id", nullable = false)
  private UUID user2Id;

  @Column(name = "last_message_id")
  private UUID lastMessageId;

  @Column(name = "last_message_at")
  private Instant lastMessageAt;

  @Column(name = "last_message_content", columnDefinition = "TEXT")
  private String lastMessageContent;

  @Column(name = "last_message_sender_id")
  private UUID lastMessageSenderId;

    protected Conversation() {
    }

    private Conversation(UUID user1Id, UUID user2Id) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
    }

    /// 자기자신과 대화방 생성x
    public static Conversation create(UUID userAId, UUID userBId) {
        // 사용자 ID null 검증
        Objects.requireNonNull(userAId, "userAId는 null일 수 없습니다.");

        Objects.requireNonNull(userBId, "userBId는 null일 수 없습니다.");

        /// 자기자신과 대화방 생성x
        if (userAId.equals(userBId)) {
            throw new IllegalArgumentException("자기 자신과 대화방을 생성할 수 없습니다.");
        }

        /// 두 UUID를 항상 같은 순서로 정렬하기위한 로직
        /// PostgreSQL UUID CHECK (user1_id < user2_id) 제약과 같은 문자열 기준으로 정렬한다.
        boolean userAIsFirst = userAId.toString().compareTo(userBId.toString()) < 0;
        UUID user1Id = userAIsFirst ? userAId : userBId;
        UUID user2Id = userAIsFirst ? userBId : userAId;

        return new Conversation(user1Id, user2Id);
    }

    public void updateLastMessage(UUID messageId, Instant createdAt, String content, UUID senderId) {
        this.lastMessageId = Objects.requireNonNull(messageId, "메시지 ID는 null일 수 없습니다.");
        this.lastMessageAt = Objects.requireNonNull(createdAt, "메시지 생성 시각은 null일 수 없습니다.");
        this.lastMessageContent = content;
        this.lastMessageSenderId = Objects.requireNonNull(senderId, "발신자 ID는 null일 수 없습니다.");
    }
}
