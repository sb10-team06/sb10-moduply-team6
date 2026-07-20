package com.team6.moduply.conversation.repository.qdsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.conversation.dto.ConversationFindAllRequest;
import com.team6.moduply.conversation.dto.ConversationListItemDto;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.entity.QConversationUserState;
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
  private final QConversationUserState conversationUserState = QConversationUserState.conversationUserState;
  private final QUser user1 = new QUser("conversationUser1");
  private final QUser user2 = new QUser("conversationUser2");
  private final QUser withUser = new QUser("conversationWithUser");

  @Override
  public List<Conversation> findAllWithCursor(
      ConversationFindAllRequest request,
      UUID currentUserId
  ) {
    // 검색어 없으면 조인 X
    if (!StringUtils.hasText(request.keywordLike())) {
      return queryFactory.selectFrom(conversation)
          .where(
              participantCondition(currentUserId),
              cursorCondition(request)
          )
          .orderBy(createdAtOrder(request.sortDirection()), idOrder(request.sortDirection()))
          .limit(request.limit() + 1)
          .fetch();
    }

    return queryFactory.selectFrom(conversation)
        .leftJoin(user1).on(user1.id.eq(conversation.user1Id))
        .leftJoin(user2).on(user2.id.eq(conversation.user2Id))
        .where(
            participantCondition(currentUserId),
            keywordLikeCondition(request.keywordLike(), currentUserId),
            cursorCondition(request)
        )
        .orderBy(createdAtOrder(request.sortDirection()), idOrder(request.sortDirection()))
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public List<ConversationListItemDto> findAllDtoWithCursor(
      ConversationFindAllRequest request,
      UUID currentUserId
  ) {
    return queryFactory
        .select(Projections.constructor(
            ConversationListItemDto.class,
            conversation.id,
            lastActivityAt(),
            withUser,
            conversation.lastMessageId,
            conversation.lastMessageAt,
            conversation.lastMessageContent,
            conversation.lastMessageSenderId,
            conversationUserState.unreadCount.coalesce(0L)
        ))
        .from(conversation)
        .join(withUser).on(withUserCondition(currentUserId))
        .leftJoin(withUser.profileImg).fetchJoin()
        .leftJoin(conversationUserState).on(
            conversationUserState.conversation.eq(conversation),
            conversationUserState.userId.eq(currentUserId)
        )
        .leftJoin(user1).on(user1.id.eq(conversation.user1Id))
        .leftJoin(user2).on(user2.id.eq(conversation.user2Id))
        .where(
            participantCondition(currentUserId),
            keywordLikeCondition(request.keywordLike(), currentUserId),
            dtoCursorCondition(request)
        )
        .orderBy(lastActivityAtOrder(request.sortDirection()), idOrder(request.sortDirection()))
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public long countWithCondition(ConversationFindAllRequest request, UUID currentUserId) {
    // 검색어 없을때는 조인 없이 COUNT
    if (!StringUtils.hasText(request.keywordLike())) {
      Long result = queryFactory.select(conversation.count())
          .from(conversation)
          .where(participantCondition(currentUserId))
          .fetchOne();

      return result != null ? result : 0L;
    }

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

  private BooleanExpression withUserCondition(UUID currentUserId) {
    return conversation.user1Id.eq(currentUserId).and(withUser.id.eq(conversation.user2Id))
        .or(conversation.user2Id.eq(currentUserId).and(withUser.id.eq(conversation.user1Id)));
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
        .or(conversation.createdAt.eq(cursor).and(conversation.id.lt(request.idAfter())));
  }

  private BooleanExpression dtoCursorCondition(ConversationFindAllRequest request) {
    if (request.cursor() == null) {
      return null;
    }

    Instant cursor = Instant.parse(request.cursor());
    DateTimeExpression<Instant> lastActivityAt = lastActivityAt();
    if (request.sortDirection() == SortDirection.ASCENDING) {
      return lastActivityAt.gt(cursor)
          .or(lastActivityAt.eq(cursor).and(conversation.id.gt(request.idAfter())));
    }

    return lastActivityAt.lt(cursor)
        .or(lastActivityAt.eq(cursor).and(conversation.id.lt(request.idAfter())));
  }

  private OrderSpecifier<Instant> createdAtOrder(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING
        ? conversation.createdAt.asc()
        : conversation.createdAt.desc();
  }

  private OrderSpecifier<Instant> lastActivityAtOrder(SortDirection sortDirection) {
    DateTimeExpression<Instant> lastActivityAt = lastActivityAt();
    return sortDirection == SortDirection.ASCENDING
        ? lastActivityAt.asc()
        : lastActivityAt.desc();
  }

  private OrderSpecifier<UUID> idOrder(SortDirection sortDirection) {
    return sortDirection == SortDirection.ASCENDING
        ? conversation.id.asc()
        : conversation.id.desc();
  }

  private DateTimeExpression<Instant> lastActivityAt() {
    return conversation.lastMessageAt.coalesce(conversation.createdAt);
  }
}
