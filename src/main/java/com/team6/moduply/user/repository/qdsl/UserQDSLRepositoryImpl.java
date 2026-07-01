package com.team6.moduply.user.repository.qdsl;

import static com.team6.moduply.user.entity.QUser.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.enums.UserSortBy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class UserQDSLRepositoryImpl implements UserQDSLRepository {
  private final JPAQueryFactory queryFactory;

  @Override
  public List<User> findUsers(String keyword, UserSortBy orderBy, SortDirection direction, String cursor,
      UUID idAfter, int limit) {
    return queryFactory
        .selectFrom(user)
        .where(containKeyword(keyword), cursorCondition(cursor, idAfter, orderBy, direction))
        // 같은 정렬 값 안에서 응답 순서를 고정하기 위한 보조 정렬이다.
        // UUID는 의미 있는 순서를 갖지 않지만, 커서 경계의 tie-breaker로만 사용한다.
        .orderBy(dynamicOrder(orderBy, direction), user.id.asc())
        .limit(limit + 1)
        .fetch();
  }

  @Override
  public long countUsers(String keyword) {
    Long count = queryFactory
        .select(user.count())
        .from(user)
        .where(containKeyword(keyword))
        .fetchOne();
    return count != null ? count : 0L;
  }

  private BooleanBuilder containKeyword(String keyword){
    if(!StringUtils.hasText(keyword)){
      return null;
    }
    BooleanBuilder builder = new BooleanBuilder();
    return builder
        .or(user.name.containsIgnoreCase(keyword))
        .or(user.email.containsIgnoreCase(keyword));
  }

  private OrderSpecifier<?> dynamicOrder(UserSortBy orderBy, SortDirection direction){
    Order order = direction.equals(SortDirection.ASCENDING) ? Order.ASC : Order.DESC;
    UserSortBy sortBy = orderBy == null ? UserSortBy.name : orderBy;

    return switch(sortBy){
      case name -> new OrderSpecifier<>(order, user.name);
      case email -> new OrderSpecifier<>(order, user.email);
      case createdAt -> new OrderSpecifier<>(order, user.createdAt);
      case isLocked -> new OrderSpecifier<>(order, lockedRank());
      case role -> new OrderSpecifier<>(order, user.role);
    };
  }

  private BooleanExpression cursorCondition(String cursor, UUID idAfter, UserSortBy orderBy, SortDirection direction){
    if(cursor == null && idAfter == null){
      // 커서 값이 없으면 조건 무시
      return null;
    }

    boolean isAscending = direction.equals(SortDirection.ASCENDING);
    UserSortBy sortBy = orderBy == null ? UserSortBy.name : orderBy;

    return switch(sortBy){
      case name -> {
        yield isAscending
            ? user.name.gt(cursor).or(user.name.eq(cursor).and(user.id.gt(idAfter)))
            : user.name.lt(cursor).or(user.name.eq(cursor).and(user.id.gt(idAfter)));
      }
      case email -> {
        yield isAscending
            ? user.email.gt(cursor).or(user.email.eq(cursor).and(user.id.gt(idAfter)))
            : user.email.lt(cursor).or(user.email.eq(cursor).and(user.id.gt(idAfter)));
      }
      case createdAt -> {
        Instant createdAtCursor = Instant.parse(cursor);
        yield isAscending
            ? user.createdAt.gt(createdAtCursor).or(user.createdAt.eq(createdAtCursor).and(user.id.gt(idAfter)))
            : user.createdAt.lt(createdAtCursor).or(user.createdAt.eq(createdAtCursor).and(user.id.gt(idAfter)));
      }
      case isLocked -> {
        int cursorRank = Boolean.parseBoolean(cursor) ? 0 : 1;
        NumberExpression<Integer> lockedRank = lockedRank();
        yield isAscending
            ? lockedRank.gt(cursorRank).or(lockedRank.eq(cursorRank).and(user.id.gt(idAfter)))
            : lockedRank.lt(cursorRank).or(lockedRank.eq(cursorRank).and(user.id.gt(idAfter)));
      }
      case role -> {
        Role cursorRole = Role.valueOf(cursor);
        yield isAscending
            ? user.role.gt(cursorRole).or(user.role.eq(cursorRole).and(user.id.gt(idAfter)))
            : user.role.lt(cursorRole).or(user.role.eq(cursorRole).and(user.id.gt(idAfter)));
      }
    };
  }

  private NumberExpression<Integer> lockedRank() {
    return new CaseBuilder()
        .when(user.isBlocked.isTrue()).then(0)
        .otherwise(1);
  }


}
