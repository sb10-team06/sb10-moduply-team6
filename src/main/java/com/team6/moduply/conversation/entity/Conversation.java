package com.team6.moduply.conversation.entity;

import com.team6.moduply.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;

@Entity
@Getter
@Table(
    name = "conversations",
    /// (user1, user2)유니크 제약조건: 대화방 중복 제거
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_conversations_user_pair",
            columnNames = {"user1_id", "user2_id"}
        )
    }
)
public class Conversation extends BaseEntity {

  @Column(name = "user1_id", nullable = false)
  private UUID user1Id;

  @Column(name = "user2_id", nullable = false)
  private UUID user2Id;
}
