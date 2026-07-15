package com.team6.moduply.testdata.generator;

import com.team6.moduply.testdata.ConversationTestDataProperties;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@Profile("data-gen")
@RequiredArgsConstructor
public class ConversationGenerator {

  private static final String SELECT_USERS_SQL = """
      select id
      from users
      where email like ?
      order by email
      limit ?
      """;

  private static final String COUNT_GENERATED_CONVERSATIONS_SQL = """
      select count(*)
      from conversations c
      join users u1 on u1.id = c.user1_id
      join users u2 on u2.id = c.user2_id
      where u1.email like ? and u2.email like ?
      """;

  private static final String INSERT_CONVERSATION_SQL = """
      insert into conversations (
        id, user1_id, user2_id, created_at, updated_at
      ) values (?, ?, ?, ?, ?)
      """;

  private static final String INSERT_DIRECT_MESSAGE_SQL = """
      insert into direct_messages (
        id, conversation_id, sender_id, content, is_read, created_at, updated_at
      ) values (?, ?, ?, ?, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;
  private final ConversationTestDataProperties properties;

  public void generate() {
    if (!properties.isEnabled()) {
      log.info("[ConversationGenerator] disabled. skip generation.");
      return;
    }

    validateProperties();

    long existingCount = countGeneratedConversations();
    if (properties.isSkipIfExists() && existingCount > 0) {
      log.info("[ConversationGenerator] generated conversations already exist. count={}",
          existingCount);
      return;
    }

    List<UUID> userIds = loadUserIds();
    int offsetCount = (properties.getConversationsPerUser() + 1) / 2;
    long expectedConversationCount = (long) userIds.size() * offsetCount;
    long expectedMessageCount =
        expectedConversationCount * properties.getMessagesPerConversation();

    log.info(
        "[ConversationGenerator] start generation. userSize={}, conversationsPerUserAtLeast={}, "
            + "offsetCount={}, conversations={}, messages={}, chunkSize={}",
        userIds.size(),
        offsetCount * 2,
        offsetCount,
        expectedConversationCount,
        expectedMessageCount,
        properties.getChunkSize()
    );

    List<ConversationSeed> chunk = new ArrayList<>(properties.getChunkSize());
    long createdConversationCount = 0;

    for (int userIndex = 0; userIndex < userIds.size(); userIndex += 1) {
      for (int offset = 1; offset <= offsetCount; offset += 1) {
        int otherUserIndex = (userIndex + offset) % userIds.size();
        chunk.add(createConversationSeed(userIds.get(userIndex), userIds.get(otherUserIndex)));

        if (chunk.size() >= properties.getChunkSize()) {
          insertChunk(chunk);
          createdConversationCount += chunk.size();
          chunk.clear();
        }
      }
    }

    if (!chunk.isEmpty()) {
      insertChunk(chunk);
      createdConversationCount += chunk.size();
    }

    log.info("[ConversationGenerator] completed. conversations={}, messages={}",
        createdConversationCount,
        createdConversationCount * properties.getMessagesPerConversation());
  }

  private void validateProperties() {
    if (properties.getUserSize() < 2) {
      throw new IllegalArgumentException("conversation userSize must be at least 2.");
    }
    if (properties.getConversationsPerUser() < 1) {
      throw new IllegalArgumentException("conversationsPerUser must be positive.");
    }
    if (properties.getMessagesPerConversation() < 0) {
      throw new IllegalArgumentException("messagesPerConversation must not be negative.");
    }
    if (properties.getChunkSize() < 1) {
      throw new IllegalArgumentException("conversation chunkSize must be positive.");
    }
    if (properties.getUnreadRatio() < 0 || properties.getUnreadRatio() > 1) {
      throw new IllegalArgumentException("unreadRatio must be between 0 and 1.");
    }
  }

  private long countGeneratedConversations() {
    Long count = jdbcTemplate.queryForObject(
        COUNT_GENERATED_CONVERSATIONS_SQL,
        Long.class,
        properties.getUserEmailLike(),
        properties.getUserEmailLike()
    );
    return count == null ? 0 : count;
  }

  private List<UUID> loadUserIds() {
    List<UUID> userIds = jdbcTemplate.query(
        SELECT_USERS_SQL,
        (rs, rowNum) -> rs.getObject("id", UUID.class),
        properties.getUserEmailLike(),
        properties.getUserSize()
    );

    if (userIds.size() < properties.getUserSize()) {
      throw new IllegalStateException(
          "Not enough generated users. required=%d, actual=%d, userEmailLike=%s"
              .formatted(properties.getUserSize(), userIds.size(), properties.getUserEmailLike())
      );
    }

    if (properties.getConversationsPerUser() >= userIds.size()) {
      throw new IllegalArgumentException(
          "conversationsPerUser must be less than userSize. conversationsPerUser=%d, userSize=%d"
              .formatted(properties.getConversationsPerUser(), userIds.size())
      );
    }

    int offsetCount = (properties.getConversationsPerUser() + 1) / 2;
    int maxOffsetCount = (userIds.size() - 1) / 2;
    if (offsetCount > maxOffsetCount) {
      throw new IllegalArgumentException(
          "conversationsPerUser is too high for duplicate-free ring generation. "
              + "conversationsPerUser=%d, maxSupported=%d, userSize=%d"
                  .formatted(properties.getConversationsPerUser(), maxOffsetCount * 2, userIds.size())
      );
    }

    return userIds;
  }

  private ConversationSeed createConversationSeed(UUID userAId, UUID userBId) {
    UUID user1Id = userAId.toString().compareTo(userBId.toString()) < 0 ? userAId : userBId;
    UUID user2Id = user1Id.equals(userAId) ? userBId : userAId;
    Instant createdAt = randomCreatedAt();
    return new ConversationSeed(UUID.randomUUID(), user1Id, user2Id, createdAt);
  }

  private Instant randomCreatedAt() {
    long secondsIn180Days = 180L * 24 * 60 * 60;
    long randomSeconds = ThreadLocalRandom.current().nextLong(secondsIn180Days);
    return Instant.now().minusSeconds(randomSeconds);
  }

  private void insertChunk(List<ConversationSeed> conversations) {
    transactionTemplate.executeWithoutResult(status -> {
      jdbcTemplate.batchUpdate(
          INSERT_CONVERSATION_SQL,
          conversations,
          conversations.size(),
          this::setConversationValues
      );

      List<DirectMessageSeed> messages = createMessages(conversations);
      if (!messages.isEmpty()) {
        jdbcTemplate.batchUpdate(
            INSERT_DIRECT_MESSAGE_SQL,
            messages,
            messages.size(),
            this::setDirectMessageValues
        );
      }
    });
  }

  private void setConversationValues(PreparedStatement ps, ConversationSeed conversation)
      throws SQLException {
    ps.setObject(1, conversation.id());
    ps.setObject(2, conversation.user1Id());
    ps.setObject(3, conversation.user2Id());
    ps.setTimestamp(4, Timestamp.from(conversation.createdAt()));
    ps.setTimestamp(5, Timestamp.from(conversation.createdAt()));
  }

  private List<DirectMessageSeed> createMessages(List<ConversationSeed> conversations) {
    int messageCount = properties.getMessagesPerConversation();
    List<DirectMessageSeed> messages = new ArrayList<>(conversations.size() * messageCount);

    for (ConversationSeed conversation : conversations) {
      for (int index = 0; index < messageCount; index += 1) {
        UUID senderId = index % 2 == 0 ? conversation.user1Id() : conversation.user2Id();
        Instant createdAt = conversation.createdAt().plusSeconds((long) index * 30);
        boolean read = ThreadLocalRandom.current().nextDouble() >= properties.getUnreadRatio();
        messages.add(new DirectMessageSeed(
            UUID.randomUUID(),
            conversation.id(),
            senderId,
            "k6 direct message %d".formatted(index + 1),
            read,
            createdAt
        ));
      }
    }

    return messages;
  }

  private void setDirectMessageValues(PreparedStatement ps, DirectMessageSeed message)
      throws SQLException {
    ps.setObject(1, message.id());
    ps.setObject(2, message.conversationId());
    ps.setObject(3, message.senderId());
    ps.setString(4, message.content());
    ps.setBoolean(5, message.read());
    ps.setTimestamp(6, Timestamp.from(message.createdAt()));
    ps.setTimestamp(7, Timestamp.from(message.createdAt()));
  }

  private record ConversationSeed(
      UUID id,
      UUID user1Id,
      UUID user2Id,
      Instant createdAt
  ) {
  }

  private record DirectMessageSeed(
      UUID id,
      UUID conversationId,
      UUID senderId,
      String content,
      boolean read,
      Instant createdAt
  ) {
  }
}
