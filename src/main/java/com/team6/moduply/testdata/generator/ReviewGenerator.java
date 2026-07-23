package com.team6.moduply.testdata.generator;

import com.team6.moduply.testdata.ReviewTestDataProperties;
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
public class ReviewGenerator {

  private static final String SELECT_CONTENT_IDS_SQL = """
      select id
      from contents
      where external_api_id like ?
      order by watcher_count desc, id
      limit ?
      """;

  private static final String SELECT_USER_IDS_SQL = """
      select id
      from users
      where email like ?
      order by email
      limit ?
      """;

  private static final String COUNT_GENERATED_REVIEWS_SQL = """
      select count(*)
      from reviews r
      join contents c on c.id = r.content_id
      join users u on u.id = r.author_id
      where c.external_api_id like ?
        and u.email like ?
      """;

  private static final String INSERT_REVIEW_SQL = """
      insert into reviews (
        id, content_id, author_id, text, rating, created_at, updated_at
      ) values (?, ?, ?, ?, ?, ?, ?)
      on conflict (content_id, author_id) do nothing
      """;

  private static final String REFRESH_CONTENT_REVIEW_STATS_SQL = """
      update contents c
      set review_count = stats.review_count,
          average_rating = round(stats.average_rating::numeric, 2),
          updated_at = ?
      from (
        select content_id,
               count(*)::int as review_count,
               coalesce(avg(rating), 0) as average_rating
        from reviews
        where content_id = ?
        group by content_id
      ) stats
      where c.id = stats.content_id
      """;

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;
  private final ReviewTestDataProperties properties;

  public void generate() {
    if (!properties.isEnabled()) {
      log.info("[ReviewGenerator] disabled. skip generation.");
      return;
    }

    validateProperties();

    long existingCount = countGeneratedReviews();
    if (properties.isSkipIfExists() && existingCount > 0) {
      log.info("[ReviewGenerator] generated reviews already exist. count={}", existingCount);
      return;
    }

    List<UUID> contentIds = loadContentIds();
    List<UUID> authorIds = loadAuthorIds();
    long expectedReviewCount = (long) contentIds.size() * properties.getReviewsPerContent();

    log.info(
        "[ReviewGenerator] start generation. hotContentSize={}, reviewsPerContent={}, totalReviews={}, chunkSize={}",
        contentIds.size(),
        properties.getReviewsPerContent(),
        expectedReviewCount,
        properties.getChunkSize()
    );
    log.info("[ReviewGenerator] hot content ids for k6 CONTENT_IDS={}", joinIds(contentIds));

    List<ReviewSeed> chunk = new ArrayList<>(properties.getChunkSize());
    long createdReviewCount = 0;

    for (UUID contentId : contentIds) {
      for (int authorIndex = 0; authorIndex < properties.getReviewsPerContent(); authorIndex += 1) {
        chunk.add(createReviewSeed(contentId, authorIds.get(authorIndex), authorIndex));

        if (chunk.size() >= properties.getChunkSize()) {
          createdReviewCount += insertChunk(chunk);
          chunk.clear();
        }
      }
    }

    if (!chunk.isEmpty()) {
      createdReviewCount += insertChunk(chunk);
    }

    refreshContentReviewStats(contentIds);

    log.info("[ReviewGenerator] completed. insertedReviews={}, requestedReviews={}, contentIds={}",
        createdReviewCount, expectedReviewCount, joinIds(contentIds));
  }

  private void validateProperties() {
    if (properties.getHotContentSize() < 1) {
      throw new IllegalArgumentException("review hotContentSize must be positive.");
    }
    if (properties.getReviewsPerContent() < 1) {
      throw new IllegalArgumentException("reviewsPerContent must be positive.");
    }
    if (properties.getChunkSize() < 1) {
      throw new IllegalArgumentException("review chunkSize must be positive.");
    }
  }

  private long countGeneratedReviews() {
    Long count = jdbcTemplate.queryForObject(
        COUNT_GENERATED_REVIEWS_SQL,
        Long.class,
        properties.getContentExternalApiIdLike(),
        properties.getUserEmailLike()
    );
    return count == null ? 0 : count;
  }

  private List<UUID> loadContentIds() {
    List<UUID> contentIds = jdbcTemplate.query(
        SELECT_CONTENT_IDS_SQL,
        (rs, rowNum) -> rs.getObject("id", UUID.class),
        properties.getContentExternalApiIdLike(),
        properties.getHotContentSize()
    );

    if (contentIds.size() < properties.getHotContentSize()) {
      throw new IllegalStateException(
          "Not enough generated contents. required=%d, actual=%d, contentExternalApiIdLike=%s"
              .formatted(
                  properties.getHotContentSize(),
                  contentIds.size(),
                  properties.getContentExternalApiIdLike()
              )
      );
    }

    return contentIds;
  }

  private List<UUID> loadAuthorIds() {
    List<UUID> authorIds = jdbcTemplate.query(
        SELECT_USER_IDS_SQL,
        (rs, rowNum) -> rs.getObject("id", UUID.class),
        properties.getUserEmailLike(),
        properties.getReviewsPerContent()
    );

    if (authorIds.size() < properties.getReviewsPerContent()) {
      throw new IllegalStateException(
          "Not enough generated users. required=%d, actual=%d, userEmailLike=%s"
              .formatted(
                  properties.getReviewsPerContent(),
                  authorIds.size(),
                  properties.getUserEmailLike()
              )
      );
    }

    return authorIds;
  }

  private ReviewSeed createReviewSeed(UUID contentId, UUID authorId, int authorIndex) {
    Instant createdAt = randomCreatedAt();
    double rating = randomRating();
    return new ReviewSeed(
        UUID.randomUUID(),
        contentId,
        authorId,
        "k6 review content=%s authorIndex=%d rating=%.1f".formatted(contentId, authorIndex + 1, rating),
        rating,
        createdAt
    );
  }

  private long insertChunk(List<ReviewSeed> reviews) {
    return transactionTemplate.execute(status -> {
      int[][] results = jdbcTemplate.batchUpdate(
          INSERT_REVIEW_SQL,
          reviews,
          reviews.size(),
          this::setReviewValues
      );

      long insertedCount = 0;
      for (int[] batchResults : results) {
        for (int result : batchResults) {
          if (result > 0) {
            insertedCount += result;
          }
        }
      }
      return insertedCount;
    });
  }

  private void setReviewValues(PreparedStatement ps, ReviewSeed review) throws SQLException {
    ps.setObject(1, review.id());
    ps.setObject(2, review.contentId());
    ps.setObject(3, review.authorId());
    ps.setString(4, review.text());
    ps.setDouble(5, review.rating());
    ps.setTimestamp(6, Timestamp.from(review.createdAt()));
    ps.setTimestamp(7, Timestamp.from(review.createdAt()));
  }

  private void refreshContentReviewStats(List<UUID> contentIds) {
    Instant updatedAt = Instant.now();
    transactionTemplate.executeWithoutResult(status -> {
      for (UUID contentId : contentIds) {
        jdbcTemplate.update(
            REFRESH_CONTENT_REVIEW_STATS_SQL,
            Timestamp.from(updatedAt),
            contentId
        );
      }
    });
  }

  private Instant randomCreatedAt() {
    long secondsIn180Days = 180L * 24 * 60 * 60;
    long randomSeconds = ThreadLocalRandom.current().nextLong(secondsIn180Days);
    return Instant.now().minusSeconds(randomSeconds);
  }

  private double randomRating() {
    return ThreadLocalRandom.current().nextInt(1, 11) / 2.0;
  }

  private String joinIds(List<UUID> ids) {
    return String.join(",", ids.stream().map(UUID::toString).toList());
  }

  private record ReviewSeed(
      UUID id,
      UUID contentId,
      UUID authorId,
      String text,
      double rating,
      Instant createdAt
  ) {
  }
}
