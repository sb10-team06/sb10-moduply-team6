package com.team6.moduply.directmessage.entity;

import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "direct_messages")
public class DirectMessage extends BaseUpdatableEntity {

  /// Conversation : DM = 1 : N
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  /// User : DM = 1 : N
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "is_read", nullable = false)
  private boolean read = false;

  protected DirectMessage() {
  }

  public void markAsRead() {
    this.read = true;
  }
}
