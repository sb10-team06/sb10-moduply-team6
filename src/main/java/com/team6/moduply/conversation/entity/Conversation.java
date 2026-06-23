package com.team6.moduply.conversation.entity;

import com.team6.moduply.common.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
    }
)
public class Conversation extends BaseEntity {

  @Column(name = "user1_id", nullable = false)
  private UUID user1Id;

  @Column(name = "user2_id", nullable = false)
  private UUID user2Id;

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
        /// 항상 작은 UUID를 user1_id에 넣도록한다.
        /// ex) user1_id = aaa, user2_id = bbb
        UUID user1Id = userAId.compareTo(userBId) < 0
                ? userAId
                : userBId;

        UUID user2Id = userAId.compareTo(userBId) < 0
                ? userBId
                : userAId;

        return new Conversation(user1Id, user2Id);
    }
}
