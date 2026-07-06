package com.team6.moduply.directmessage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.conversation.repository.ConversationRepository;
import com.team6.moduply.directmessage.dto.DirectMessageFindAllRequest;
import com.team6.moduply.directmessage.dto.DirectMessageSortBy;
import com.team6.moduply.directmessage.entity.DirectMessage;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@Import(JpaAuditingConfig.class)
class DirectMessageRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private DirectMessageRepository directMessageRepository;

  @Autowired
  private ConversationRepository conversationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("대화방 ID 목록이 비어 있으면 최신 DM 목록으로 빈 목록을 반환한다.")
  void findLatestMessagesByConversationIds_success_when_conversation_ids_empty() {
    List<DirectMessage> result = directMessageRepository.findLatestMessagesByConversationIds(List.of());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("대화방 ID 목록으로 조회하면 각 대화방의 최신 DM만 반환한다.")
  void findLatestMessagesByConversationIds_success_with_latest_message_per_conversation()
      throws Exception {
    User userA = saveUser("latest-a@example.com", "latestA");
    User userB = saveUser("latest-b@example.com", "latestB");
    User userC = saveUser("latest-c@example.com", "latestC");
    Conversation firstConversation = saveConversation(userA, userB);
    Conversation secondConversation = saveConversation(userA, userC);
    saveDirectMessage(
        firstConversation,
        userA,
        "old",
        Instant.parse("2026-01-01T00:00:00Z")
    );
    DirectMessage firstLatest = saveDirectMessage(
        firstConversation,
        userB,
        "new",
        Instant.parse("2026-01-02T00:00:00Z")
    );
    DirectMessage secondLatest = saveDirectMessage(
        secondConversation,
        userC,
        "only",
        Instant.parse("2026-01-03T00:00:00Z")
    );

    List<DirectMessage> result = directMessageRepository.findLatestMessagesByConversationIds(
        List.of(firstConversation.getId(), secondConversation.getId())
    );

    assertThat(result)
        .extracting(DirectMessage::getId)
        .containsExactlyInAnyOrder(firstLatest.getId(), secondLatest.getId());
  }

  @Test
  @DisplayName("DM 목록을 내림차순으로 조회하면 커서 이후 목록을 반환한다.")
  void findAllWithCursor_success_with_created_at_descending_sort() throws Exception {
    User sender = saveUser("dm-desc-sender@example.com", "sender");
    User receiver = saveUser("dm-desc-receiver@example.com", "receiver");
    Conversation conversation = saveConversation(sender, receiver);
    DirectMessage oldMessage = saveDirectMessage(
        conversation,
        sender,
        "old",
        Instant.parse("2026-01-01T00:00:00Z")
    );
    DirectMessage middleMessage = saveDirectMessage(
        conversation,
        receiver,
        "middle",
        Instant.parse("2026-01-02T00:00:00Z")
    );
    DirectMessage newMessage = saveDirectMessage(
        conversation,
        sender,
        "new",
        Instant.parse("2026-01-03T00:00:00Z")
    );

    List<DirectMessage> firstPage = directMessageRepository.findAllWithCursor(
        request(null, null, 10, SortDirection.DESCENDING),
        conversation.getId()
    );
    List<DirectMessage> nextPage = directMessageRepository.findAllWithCursor(
        request(newMessage.getCreatedAt().toString(), newMessage.getId(), 10,
            SortDirection.DESCENDING),
        conversation.getId()
    );

    assertThat(firstPage)
        .extracting(DirectMessage::getId)
        .containsExactly(newMessage.getId(), middleMessage.getId(), oldMessage.getId());
    assertThat(nextPage)
        .extracting(DirectMessage::getId)
        .containsExactly(middleMessage.getId(), oldMessage.getId());
  }

  @Test
  @DisplayName("DM 목록을 오름차순으로 조회하면 커서 이후 목록을 반환한다.")
  void findAllWithCursor_success_with_created_at_ascending_sort() throws Exception {
    User sender = saveUser("dm-asc-sender@example.com", "sender");
    User receiver = saveUser("dm-asc-receiver@example.com", "receiver");
    Conversation conversation = saveConversation(sender, receiver);
    DirectMessage oldMessage = saveDirectMessage(
        conversation,
        sender,
        "old",
        Instant.parse("2026-01-01T00:00:00Z")
    );
    DirectMessage middleMessage = saveDirectMessage(
        conversation,
        receiver,
        "middle",
        Instant.parse("2026-01-02T00:00:00Z")
    );
    DirectMessage newMessage = saveDirectMessage(
        conversation,
        sender,
        "new",
        Instant.parse("2026-01-03T00:00:00Z")
    );

    List<DirectMessage> firstPage = directMessageRepository.findAllWithCursor(
        request(null, null, 10, SortDirection.ASCENDING),
        conversation.getId()
    );
    List<DirectMessage> nextPage = directMessageRepository.findAllWithCursor(
        request(oldMessage.getCreatedAt().toString(), oldMessage.getId(), 10,
            SortDirection.ASCENDING),
        conversation.getId()
    );

    assertThat(firstPage)
        .extracting(DirectMessage::getId)
        .containsExactly(oldMessage.getId(), middleMessage.getId(), newMessage.getId());
    assertThat(nextPage)
        .extracting(DirectMessage::getId)
        .containsExactly(middleMessage.getId(), newMessage.getId());
  }

  @Test
  @DisplayName("createdAt이 같은 DM은 ID 보조 커서로 중복 없이 이어서 조회한다.")
  void findAllWithCursor_success_with_id_tie_breaker_when_created_at_is_same() throws Exception {
    User sender = saveUser("dm-tie-sender@example.com", "sender");
    User receiver = saveUser("dm-tie-receiver@example.com", "receiver");
    Conversation conversation = saveConversation(sender, receiver);
    Instant sameCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
    DirectMessage firstSameCreatedAt = saveDirectMessage(
        conversation,
        sender,
        "first",
        sameCreatedAt
    );
    DirectMessage secondSameCreatedAt = saveDirectMessage(
        conversation,
        receiver,
        "second",
        sameCreatedAt
    );
    DirectMessage laterMessage = saveDirectMessage(
        conversation,
        sender,
        "later",
        Instant.parse("2026-01-02T00:00:00Z")
    );
    List<DirectMessage> firstPage = directMessageRepository.findAllWithCursor(
        request(null, null, 10, SortDirection.ASCENDING),
        conversation.getId()
    );
    DirectMessage cursorMessage = firstPage.get(0);
    DirectMessage nextSameCreatedAtMessage = firstPage.get(1);

    List<DirectMessage> result = directMessageRepository.findAllWithCursor(
        request(sameCreatedAt.toString(), cursorMessage.getId(), 10,
            SortDirection.ASCENDING),
        conversation.getId()
    );

    assertThat(List.of(firstSameCreatedAt.getId(), secondSameCreatedAt.getId()))
        .containsExactlyInAnyOrder(cursorMessage.getId(), nextSameCreatedAtMessage.getId());
    assertThat(firstPage)
        .extracting(DirectMessage::getId)
        .containsExactly(cursorMessage.getId(), nextSameCreatedAtMessage.getId(),
            laterMessage.getId());
    assertThat(result)
        .extracting(DirectMessage::getId)
        .containsExactly(nextSameCreatedAtMessage.getId(), laterMessage.getId());
  }

  @Test
  @DisplayName("특정 대화방의 DM 개수를 반환한다.")
  void countWithCondition_success_with_conversation_id() throws Exception {
    User userA = saveUser("count-a@example.com", "countA");
    User userB = saveUser("count-b@example.com", "countB");
    User userC = saveUser("count-c@example.com", "countC");
    Conversation targetConversation = saveConversation(userA, userB);
    Conversation otherConversation = saveConversation(userA, userC);
    saveDirectMessage(targetConversation, userA, "first", Instant.parse("2026-01-01T00:00:00Z"));
    saveDirectMessage(targetConversation, userB, "second", Instant.parse("2026-01-02T00:00:00Z"));
    saveDirectMessage(otherConversation, userC, "other", Instant.parse("2026-01-03T00:00:00Z"));

    long result = directMessageRepository.countWithCondition(targetConversation.getId());

    assertThat(result).isEqualTo(2L);
  }

  private DirectMessageFindAllRequest request(
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection
  ) {
    return new DirectMessageFindAllRequest(
        cursor,
        idAfter,
        limit,
        sortDirection,
        DirectMessageSortBy.createdAt
    );
  }

  private User saveUser(String email, String name) {
    return userRepository.save(new User(email, "password", name, Role.USER));
  }

  private Conversation saveConversation(User userA, User userB) {
    return conversationRepository.saveAndFlush(Conversation.create(userA.getId(), userB.getId()));
  }

  private DirectMessage saveDirectMessage(
      Conversation conversation,
      User sender,
      String content,
      Instant createdAt
  ) throws Exception {
    Constructor<DirectMessage> constructor = DirectMessage.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    DirectMessage directMessage = constructor.newInstance();
    ReflectionTestUtils.setField(directMessage, "conversation", conversation);
    ReflectionTestUtils.setField(directMessage, "sender", sender);
    ReflectionTestUtils.setField(directMessage, "content", content);

    DirectMessage saved = directMessageRepository.saveAndFlush(directMessage);
    entityManager.createNativeQuery("""
            update direct_messages
            set created_at = :createdAt,
                updated_at = :createdAt
            where id = :id
            """)
        .setParameter("createdAt", createdAt)
        .setParameter("id", saved.getId())
        .executeUpdate();
    entityManager.flush();
    entityManager.clear();

    return directMessageRepository.findById(saved.getId()).orElseThrow();
  }
}
