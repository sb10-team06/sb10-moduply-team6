package com.team6.moduply.conversation.repository.qdsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.conversation.dto.ConversationFindAllRequest;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.entity.QConversation;
import com.team6.moduply.user.entity.QUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class ConversationQDSLRepositoryImpl implements ConversationQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private final QConversation conversation = QConversation.conversation;
  private final QUser user1 = new QUser("conversationUser1");
  private final QUser user2 = new QUser("conversationUser2");

  @Override
  public List<Conversation> findAllWithCursor(
      ConversationFindAllRequest request,
      UUID currentUserId
  ) {
    return queryFactory.selectFrom(conversation)
        .leftJoin(user1).on(user1.id.eq(conversation.user1Id))
        .leftJoin(user2).on(user2.id.eq(conversation.user2Id))
        .where(
            participantCondition(currentUserId),
            keywordLikeCondition(request.keywordLike(), currentUserId),
            cursorCondition(request)
        )
        .orderBy(createdAtOrder(request.sortDirection()), conversation.id.asc())
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public long countWithCondition(ConversationFindAllRequest request, UUID currentUserId) {
    Long result = queryFactory.select(conversation.count())
        .from(conversation)
        .leftJoin(user1).on(user1.id.eq(conversation.user1Id))
        .leftJoin(user2).on(user2.id.eq(conversation.user2Id))
        .where(
            participantCondition(currentUserId),
            keywordLikeCondition(request.keywordLike(), currentUserId)
        )
        .fetchOne();

    return result != null ? result : 0L;
  }

  private BooleanExpression participantCondition(UUID currentUserId) {
    return conversation.user1Id.eq(currentUserId)
        .or(conversation.user2Id.eq(currentUserId));
  }

  /// 내 이름, 이메일이 아닌 상대방 이름, 이메일로 검색한다.
  /// 내가 user_1이면 user_2정보를 검색
  /// 내가 user_2이면 user_1정보를 검색
  private BooleanExpression keywordLikeCondition(String keyword, UUID currentUserId) {
    if (!StringUtils.hasText(keyword)) {
      return null;
    }

    BooleanExpression user1Matched = user1.name.containsIgnoreCase(keyword)
        .or(user1.email.containsIgnoreCase(keyword));
    BooleanExpression user2Matched = user2.name.containsIgnoreCase(keyword)
        .or(user2.email.containsIgnoreCase(keyword));

    return conversation.user1Id.eq(currentUserId).and(user2Matched)
        .or(conversation.user2Id.eq(currentUserId).and(user1Matched));
  }

  private BooleanExpression cursorCondition(ConversationFindAllRequest request) {
    if (request.cursor() == null) {
      return null;
    }

    Instant cursor = Instant.parse(request.cursor());
    if (request.sortDirection() == SortDirection.ASCENDING) {
      return conversation.createdAt.gt(cursor)
          .or(conversation.createdAt.eq(cursor).and(conversation.id.gt(request.idAfter())));
    }

    return conversation.createdAt.lt(cursor)
        .or(conversation.createdAt.eq(cursor).and(conversation.id.gt(request.idAfter())));
  }

  private OrderSpecifier<Instant> createdAtOrder(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING
        ? conversation.createdAt.asc()
        : conversation.createdAt.desc();
  }
}
