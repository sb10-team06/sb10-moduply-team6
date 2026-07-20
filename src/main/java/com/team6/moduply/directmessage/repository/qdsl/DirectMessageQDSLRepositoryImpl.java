package com.team6.moduply.directmessage.repository.qdsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.directmessage.dto.DirectMessageFindAllRequest;
import com.team6.moduply.directmessage.dto.LatestDirectMessageDto;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.entity.QDirectMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
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
  public List<LatestDirectMessageDto> findLatestMessagesByConversationIds(Collection<UUID> conversationIds) {
    /// 조회할 대화방 없으면 DB 조회x -> 바로 빈 리스트 반환
    if (conversationIds == null || conversationIds.isEmpty()) {
      return Collections.emptyList();
    }
    /// 최신 메시지 id 조회
    return findLatestMessageDtosByConversationIds(conversationIds);
  }

  @SuppressWarnings("unchecked")
  private List<LatestDirectMessageDto> findLatestMessageDtosByConversationIds(Collection<UUID> conversationIds) {
    return entityManager
        .createNativeQuery("""
            select distinct on (dm.conversation_id)
              dm.id,
              dm.conversation_id,
              dm.created_at,
              dm.sender_id,
              dm.content
            from direct_messages dm
            where dm.conversation_id IN (:conversationIds)
            order by dm.conversation_id, dm.created_at desc, dm.id desc
            """)
        .setParameter("conversationIds", conversationIds)
        .getResultList()
        .stream()
        .map(row -> toLatestDirectMessageDto((Object[]) row))
        .toList();
  }

  private LatestDirectMessageDto toLatestDirectMessageDto(Object[] row) {
    return new LatestDirectMessageDto(
        toUuid(row[0]),
        toUuid(row[1]),
        toInstant(row[2]),
        toUuid(row[3]),
        row[4] != null ? row[4].toString() : null
    );
  }

  private UUID toUuid(Object value) {
    return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
  }

  private Instant toInstant(Object value) {
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant();
    }
    return Instant.parse(value.toString());
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
