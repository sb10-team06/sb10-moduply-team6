package com.team6.moduply.follow.entity;

import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import com.team6.moduply.user.entity.User;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_follows_follower_followee",
                        columnNames = {"follower_id", "followee_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseUpdatableEntity {

    /// 팔로우를 요청한 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "follower_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_follows_follower")
    )
    private User follower;

    /**
     * 팔로우 대상 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "followee_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_follows_followee")
    )
    private User followee;

    public Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
    }

}
