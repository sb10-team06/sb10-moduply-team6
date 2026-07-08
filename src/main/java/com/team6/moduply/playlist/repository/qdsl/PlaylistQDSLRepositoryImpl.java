package com.team6.moduply.playlist.repository.qdsl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.playlist.dto.PlaylistSearchRequest;
import com.team6.moduply.playlist.dto.PlaylistSortBy;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.entity.QPlaylist;
import com.team6.moduply.playlist.entity.QPlaylistSubscription;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaylistQDSLRepositoryImpl implements PlaylistQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private final QPlaylist playlist = QPlaylist.playlist;

  @Override
  public List<Playlist> findAllWithCursor(PlaylistSearchRequest request) {
    var query = queryFactory.selectFrom(playlist)
        .where(
            keywordLikeCondition(request.keywordLike()),
            ownerIdCondition(request.ownerIdEqual()),
            cursorCondition(request),
            subscriberIdCondition(request.subscriberIdEqual())
        )
        .orderBy(
            request.sortBy() == PlaylistSortBy.createdAt
                ? (request.sortDirection() == SortDirection.ASCENDING
                ? playlist.createdAt.asc()
                : playlist.createdAt.desc())
                : (request.sortDirection() == SortDirection.ASCENDING
                    ? playlist.updatedAt.asc()
                    : playlist.updatedAt.desc()),
            playlist.id.asc()
        )
        .limit(request.limit() + 1);

    return query.fetch();
  }

  @Override
  public long countWithCondition(PlaylistSearchRequest request) {
    Long result = queryFactory.select(playlist.count())
        .from(playlist)
        .where(
            keywordLikeCondition(request.keywordLike()),
            ownerIdCondition(request.ownerIdEqual()),
            subscriberIdCondition(request.subscriberIdEqual())
        )
        .fetchOne();
    return result != null ? result : 0L;
  }

  private BooleanExpression subscriberIdCondition(UUID subscriberId) {
    if (subscriberId == null) return null;
    QPlaylistSubscription subscription = QPlaylistSubscription.playlistSubscription;
    return playlist.id.in(
        JPAExpressions.select(subscription.playlist.id)
            .from(subscription)
            .where(subscription.subscriberId.eq(subscriberId))
    );
  }

  private BooleanExpression keywordLikeCondition(String keyword) {
    return keyword != null ? playlist.title.containsIgnoreCase(keyword) : null;
  }

  private BooleanExpression ownerIdCondition(UUID ownerId) {
    return ownerId != null ? playlist.ownerId.eq(ownerId) : null;
  }

  private BooleanExpression cursorCondition(PlaylistSearchRequest request) {
    if (request.cursor() == null) {
      return null;
    }

    Instant cursorTime = Instant.parse(request.cursor());

    if (request.sortDirection() == SortDirection.ASCENDING) {
      return playlist.updatedAt.gt(cursorTime)
          .or(playlist.updatedAt.eq(cursorTime)
              .and(playlist.id.gt(request.idAfter())));
    } else {
      return playlist.updatedAt.lt(cursorTime)
          .or(playlist.updatedAt.eq(cursorTime)
              .and(playlist.id.gt(request.idAfter())));
    }
  }
}