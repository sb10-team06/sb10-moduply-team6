package com.team6.moduply.directmessage.repository.qdsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.directmessage.dto.DirectMessageFindAllRequest;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.entity.QDirectMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
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

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<DirectMessage> findLatestMessagesByConversationIds(Collection<UUID> conversationIds) {
    if (conversationIds == null || conversationIds.isEmpty()) {
      return Collections.emptyList();
    }

    List<UUID> latestMessageIds = findLatestMessageIdsByConversationIds(conversationIds);
    if (latestMessageIds.isEmpty()) {
      return Collections.emptyList();
    }

    return queryFactory.selectFrom(directMessage)
        .join(directMessage.conversation).fetchJoin()
        .join(directMessage.sender).fetchJoin()
        .where(directMessage.id.in(latestMessageIds))
        .fetch();
  }

  @SuppressWarnings("unchecked")
  private List<UUID> findLatestMessageIdsByConversationIds(Collection<UUID> conversationIds) {
    return entityManager
        .createNativeQuery("""
            select ranked.id
            from (
              select
                dm.id,
                row_number() over (
                  partition by dm.conversation_id
                  order by dm.created_at desc, dm.id desc
                ) as rn
              from direct_messages dm
              where dm.conversation_id = any(:conversationIds)
            ) ranked
            where ranked.rn = 1
            """)
        .setParameter("conversationIds", conversationIds.toArray(UUID[]::new))
        .getResultList()
        .stream()
        .map(value -> value instanceof UUID uuid ? uuid : UUID.fromString(value.toString()))
        .toList();
  }

  @Override
  public List<DirectMessage> findAllWithCursor(
      DirectMessageFindAllRequest request,
      UUID conversationId
  ) {
    return queryFactory.selectFrom(directMessage)
        .join(directMessage.conversation).fetchJoin()
        .join(directMessage.sender).fetchJoin()
        .where(
            directMessage.conversation.id.eq(conversationId),
            cursorCondition(request)
        )
        .orderBy(createdAtOrder(request.sortDirection()), idOrder(request.sortDirection()))
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public long countWithCondition(UUID conversationId) {
    Long result = queryFactory.select(directMessage.count())
        .from(directMessage)
        .where(directMessage.conversation.id.eq(conversationId))
        .fetchOne();

    return result != null ? result : 0L;
  }

  private BooleanExpression cursorCondition(DirectMessageFindAllRequest request) {
    if (request.cursor() == null) {
      return null;
    }

    Instant cursor = Instant.parse(request.cursor());
    if (request.sortDirection() == SortDirection.ASCENDING) {
      return directMessage.createdAt.gt(cursor)
          .or(directMessage.createdAt.eq(cursor).and(directMessage.id.gt(request.idAfter())));
    }

    return directMessage.createdAt.lt(cursor)
        .or(directMessage.createdAt.eq(cursor).and(directMessage.id.lt(request.idAfter())));
  }

  private OrderSpecifier<Instant> createdAtOrder(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING
        ? directMessage.createdAt.asc()
        : directMessage.createdAt.desc();
  }

  private OrderSpecifier<UUID> idOrder(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING
        ? directMessage.id.asc()
        : directMessage.id.desc();
  }
}
