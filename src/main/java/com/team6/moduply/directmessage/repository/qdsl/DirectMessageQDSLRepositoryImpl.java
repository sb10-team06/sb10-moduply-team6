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
}
