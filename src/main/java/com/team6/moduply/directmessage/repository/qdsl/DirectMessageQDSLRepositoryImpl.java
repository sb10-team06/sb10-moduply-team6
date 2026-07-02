package com.team6.moduply.directmessage.repository.qdsl;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.directmessage.dto.DirectMessageFindAllRequest;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.directmessage.entity.QDirectMessage;
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
  private final QDirectMessage latest = new QDirectMessage("latestDirectMessage");

  // TODO: self-join NOT EXISTS 쿼리 성능을 위한 인덱스 검토 권장.
  // TODO: direct_message(conversation_id, created_at desc, id desc) 복합 인덱스 추가를 권장합니다.
  /// conversation별 최신 메시지를 찾기위해 상관 서브쿼리(NOT EXISTS)를 사용한다.
  /// 로직자체는 createdAt/ id 타이브레이크로 정확해 보이지만 direct_message 테이블이 커지면
  /// conversation_id, created_at, id 복합 인덱스가 없을경우 각 대화방마드 풀스캔에 가까운 비용이 발생할 수 있다.
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

  @Override
  public List<DirectMessage> findAllWithCursor(
      DirectMessageFindAllRequest request,
      UUID conversationId
  ) {
    return queryFactory.selectFrom(directMessage)
            // fetch join으로 DM - 대화방 - 사용자 조회
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
