package com.team6.moduply.conversation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.conversation.dto.ConversationFindAllRequest;
import com.team6.moduply.conversation.dto.ConversationSortBy;
import com.team6.moduply.conversation.entity.Conversation;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaAuditingConfig.class)
class ConversationRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private ConversationRepository conversationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("대화 목록을 createdAt 내림차순으로 조회하고 커서 이후 목록을 반환한다.")
  void findAllWithCursor_success_with_created_at_descending_sort() {
    User currentUser = saveUser("current-desc@example.com", "current");
    Conversation oldConversation = saveConversation(
        currentUser,
        saveUser("old@example.com", "old"),
        Instant.parse("2026-01-01T00:00:00Z")
    );
    Conversation middleConversation = saveConversation(
        currentUser,
        saveUser("middle@example.com", "middle"),
        Instant.parse("2026-01-02T00:00:00Z")
    );
    Conversation newConversation = saveConversation(
        currentUser,
        saveUser("new@example.com", "new"),
        Instant.parse("2026-01-03T00:00:00Z")
    );

    List<Conversation> firstPage = conversationRepository.findAllWithCursor(
        request(null, null, 10, SortDirection.DESCENDING),
        currentUser.getId()
    );
    List<Conversation> nextPage = conversationRepository.findAllWithCursor(
        request(newConversation.getCreatedAt().toString(), newConversation.getId(), 10,
            SortDirection.DESCENDING),
        currentUser.getId()
    );

    assertThat(firstPage)
        .extracting(Conversation::getId)
        .containsExactly(
            newConversation.getId(),
            middleConversation.getId(),
            oldConversation.getId()
        );
    assertThat(nextPage)
        .extracting(Conversation::getId)
        .containsExactly(middleConversation.getId(), oldConversation.getId());
  }

  @Test
  @DisplayName("대화 목록을 createdAt 오름차순으로 조회하고 커서 이후 목록을 반환한다.")
  void findAllWithCursor_success_with_created_at_ascending_sort() {
    User currentUser = saveUser("current-asc@example.com", "current");
    Conversation oldConversation = saveConversation(
        currentUser,
        saveUser("asc-old@example.com", "old"),
        Instant.parse("2026-01-01T00:00:00Z")
    );
    Conversation middleConversation = saveConversation(
        currentUser,
        saveUser("asc-middle@example.com", "middle"),
        Instant.parse("2026-01-02T00:00:00Z")
    );
    Conversation newConversation = saveConversation(
        currentUser,
        saveUser("asc-new@example.com", "new"),
        Instant.parse("2026-01-03T00:00:00Z")
    );

    List<Conversation> firstPage = conversationRepository.findAllWithCursor(
        request(null, null, 10, SortDirection.ASCENDING),
        currentUser.getId()
    );
    List<Conversation> nextPage = conversationRepository.findAllWithCursor(
        request(oldConversation.getCreatedAt().toString(), oldConversation.getId(), 10,
            SortDirection.ASCENDING),
        currentUser.getId()
    );

    assertThat(firstPage)
        .extracting(Conversation::getId)
        .containsExactly(
            oldConversation.getId(),
            middleConversation.getId(),
            newConversation.getId()
        );
    assertThat(nextPage)
        .extracting(Conversation::getId)
        .containsExactly(middleConversation.getId(), newConversation.getId());
  }

  @Test
  @DisplayName("createdAt이 같은 대화 목록은 ID 보조 커서로 중복 없이 이어서 조회한다.")
  void findAllWithCursor_success_with_id_tie_breaker_when_created_at_is_same() {
    User currentUser = saveUser("current-tie@example.com", "current");
    Instant sameCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
    Conversation firstSameCreatedAt = saveConversation(
        currentUser,
        saveUser("tie-first@example.com", "first"),
        sameCreatedAt
    );
    Conversation secondSameCreatedAt = saveConversation(
        currentUser,
        saveUser("tie-second@example.com", "second"),
        sameCreatedAt
    );
    Conversation laterConversation = saveConversation(
        currentUser,
        saveUser("tie-later@example.com", "later"),
        Instant.parse("2026-01-02T00:00:00Z")
    );
    List<Conversation> firstPage = conversationRepository.findAllWithCursor(
        request(null, null, 10, SortDirection.ASCENDING),
        currentUser.getId()
    );
    Conversation cursorConversation = firstPage.get(0);
    Conversation nextConversation = firstPage.get(1);

    List<Conversation> result = conversationRepository.findAllWithCursor(
        request(sameCreatedAt.toString(), cursorConversation.getId(), 10, SortDirection.ASCENDING),
        currentUser.getId()
    );

    assertThat(firstPage)
        .extracting(Conversation::getId)
        .containsExactly(
            cursorConversation.getId(),
            nextConversation.getId(),
            laterConversation.getId()
        );
    assertThat(List.of(firstSameCreatedAt.getId(), secondSameCreatedAt.getId()))
        .containsExactlyInAnyOrder(cursorConversation.getId(), nextConversation.getId());
    assertThat(result)
        .extracting(Conversation::getId)
        .containsExactly(nextConversation.getId(), laterConversation.getId());
  }

  private ConversationFindAllRequest request(
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection
  ) {
    return new ConversationFindAllRequest(
        null,
        cursor,
        idAfter,
        limit,
        sortDirection,
        ConversationSortBy.createdAt
    );
  }

  private User saveUser(String email, String name) {
    return userRepository.save(new User(email, "password", name, Role.USER));
  }

  private Conversation saveConversation(User currentUser, User withUser, Instant createdAt) {
    Conversation conversation = conversationRepository.save(
        Conversation.create(currentUser.getId(), withUser.getId())
    );
    conversationRepository.flush();
    entityManager.createNativeQuery("""
            update conversations
            set created_at = :createdAt,
                updated_at = :createdAt
            where id = :id
            """)
        .setParameter("createdAt", createdAt)
        .setParameter("id", conversation.getId())
        .executeUpdate();
    entityManager.flush();
    entityManager.clear();

    return conversationRepository.findById(conversation.getId()).orElseThrow();
  }
}
