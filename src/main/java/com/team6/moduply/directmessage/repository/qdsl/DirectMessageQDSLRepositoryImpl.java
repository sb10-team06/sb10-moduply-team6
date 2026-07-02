package com.team6.moduply.directmessage.repository.qdsl;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.entity.QDirectMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DirectMessageQDSLRepositoryImpl implements DirectMessageQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private final QDirectMessage directMessage = QDirectMessage.directMessage;
  private final QDirectMessage latest = new QDirectMessage("latestDirectMessage");

  @Override
  public List<DirectMessage> findLatestMessagesByConversationIds(Collection<UUID> conversationIds) {
    if (conversationIds == null || conversationIds.isEmpty()) {
      return Collections.emptyList();
    }

    return queryFactory.selectFrom(directMessage)
        .join(directMessage.conversation).fetchJoin()
        .join(directMessage.sender).fetchJoin()
        .where(
            directMessage.conversation.id.in(conversationIds),
            JPAExpressions.selectOne()
                .from(latest)
                .where(
                    latest.conversation.id.eq(directMessage.conversation.id),
                    latest.createdAt.gt(directMessage.createdAt)
                        .or(latest.createdAt.eq(directMessage.createdAt)
                            .and(latest.id.gt(directMessage.id)))
                )
                .notExists()
        )
        .fetch();
  }
}
