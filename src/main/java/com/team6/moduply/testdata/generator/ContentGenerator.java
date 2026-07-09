package com.team6.moduply.testdata.generator;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.common.baseentity.BaseEntity;
import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

import com.team6.moduply.testdata.ContentTestDataProperties;
import lombok.extern.slf4j.Slf4j;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Component
@Profile("data-gen")
public class ContentGenerator extends AbstractGenerator<Content> {

  private static final String GENERATED_EXTERNAL_API_ID_PREFIX = "k6-seed-";

  private static final String INSERT_BINARY_CONTENT_SQL = """
      insert into binary_contents (
        id, file_name, size, content_type, storage_key, status, created_at, updated_at
      ) values (?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String INSERT_CONTENT_SQL = """
      insert into contents (
        id, content_img_id, external_api_id, type, title, description,
        average_rating, review_count, watcher_count, created_at, updated_at
      ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private final ContentTestDataProperties properties;
  private final TransactionTemplate transactionTemplate;

  public ContentGenerator(
      JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor dataGeneratorExecutor,
      ContentTestDataProperties properties,
      TransactionTemplate transactionTemplate
  ) {
    super(jdbcTemplate, dataGeneratorExecutor);
    this.properties = properties;
    this.transactionTemplate = transactionTemplate;
  }

  public void generate() {
    if (!properties.isEnabled()) {
      log.info("[ContentGenerator] disabled. skip generation.");
      return;
    }

    long existingCount = countGeneratedContents();
    if (properties.isSkipIfExists() && existingCount > 0) {
      log.info("[ContentGenerator] generated contents already exist. count={}", existingCount);
      return;
    }

    log.info("[ContentGenerator] start generation. totalSize={}, chunkSize={}",
        properties.getTotalSize(), properties.getChunkSize());

    generate(properties.getTotalSize(), properties.getChunkSize());
  }

  @Override
  protected Model<Content> getModel() {
    return Instancio.of(Content.class)
        .supply(Select.field(Content.class, "externalApiId"), this::createExternalApiId)
        .supply(Select.field(Content.class, "type"), this::randomType)
        .supply(Select.field(Content.class, "title"), this::createTitle)
        .supply(Select.field(Content.class, "description"), () -> faker().lorem().paragraph())
        .supply(Select.field(Content.class, "averageRating"),
            () -> BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(0, 501), 2))
        .supply(Select.field(Content.class, "reviewCount"),
            () -> ThreadLocalRandom.current().nextInt(0, 5000))
        .supply(Select.field(Content.class, "watcherCount"),
            () -> ThreadLocalRandom.current().nextLong(0, 100000))
        .supply(Select.field(Content.class, "contentImg"), this::createBinaryContent)
        .onComplete(Select.all(Content.class), this::setBaseFields)
        .toModel();
  }

  @Override
  protected String getSql() {
    return INSERT_CONTENT_SQL;
  }

  @Override
  protected void executeBatch(List<Content> contents) {
    transactionTemplate.executeWithoutResult(status -> {
      insertBinaryContents(contents);
      super.executeBatch(contents);
    });
  }

  @Override
  protected void setValues(PreparedStatement ps, Content content)
      throws SQLException {
    ps.setObject(1, content.getId());
    ps.setObject(2, content.getContentImg().getId());
    ps.setString(3, content.getExternalApiId());
    ps.setString(4, content.getType().name());
    ps.setString(5, content.getTitle());
    ps.setString(6, content.getDescription());
    ps.setBigDecimal(7, content.getAverageRating());
    ps.setInt(8, content.getReviewCount());
    ps.setLong(9, content.getWatcherCount());
    ps.setTimestamp(10, Timestamp.from(content.getCreatedAt()));
    ps.setTimestamp(11, Timestamp.from(content.getUpdatedAt()));
  }

  private long countGeneratedContents() {
    Long count = jdbcTemplate.queryForObject(
        "select count(*) from contents where external_api_id like ?",
        Long.class,
        GENERATED_EXTERNAL_API_ID_PREFIX + "%"
    );
    return count == null ? 0 : count;
  }

  private void insertBinaryContents(List<Content> contents) {
    jdbcTemplate.batchUpdate(
        INSERT_BINARY_CONTENT_SQL,
        contents,
        contents.size(),
        this::setBinaryContentValues
    );
  }

  private void setBinaryContentValues(PreparedStatement ps, Content content)
      throws SQLException {
    BinaryContent contentImg = content.getContentImg();

    ps.setObject(1, contentImg.getId());
    ps.setString(2, contentImg.getFileName());
    ps.setLong(3, contentImg.getSize());
    ps.setString(4, contentImg.getContentType());
    ps.setString(5, contentImg.getStorageKey());
    ps.setString(6, contentImg.getStatus().name());
    ps.setTimestamp(7, Timestamp.from(contentImg.getCreatedAt()));
    ps.setTimestamp(8, Timestamp.from(contentImg.getUpdatedAt()));
  }

  private ContentType randomType() {
    ContentType[] values = ContentType.values();
    return values[ThreadLocalRandom.current().nextInt(values.length)];
  }

  private String createExternalApiId() {
    return GENERATED_EXTERNAL_API_ID_PREFIX + createUniqueValue();
  }

  private String createTitle() {
    return "k6 content " + createUniqueValue();
  }

  private String createFileName() {
    return "k6-thumbnail-%s.png".formatted(createUniqueValue());
  }

  private String createStorageKey() {
    return "test-data/contents/%s/thumbnail.png".formatted(createUniqueValue());
  }

  private String createUniqueValue() {
    return "%s-%s".formatted(System.nanoTime(), UUID.randomUUID());
  }

  private BinaryContent createBinaryContent() {
    BinaryContent binaryContent = BinaryContent.create(
        createFileName(),
        68L,
        "image/png",
        createStorageKey()
    );
    binaryContent.success();
    setBaseFields(binaryContent);
    return binaryContent;
  }

  private void setBaseFields(BaseUpdatableEntity entity) {
    Instant createdAt = getUniformTimestamp(180).toInstant();
    setField(entity, BaseEntity.class, "id", UUID.randomUUID());
    setField(entity, BaseEntity.class, "createdAt", createdAt);
    setField(entity, BaseUpdatableEntity.class, "updatedAt", createdAt);
  }

  private void setField(Object target, Class<?> declaringClass, String fieldName, Object value) {
    Field field = ReflectionUtils.findField(declaringClass, fieldName);
    if (field == null) {
      throw new IllegalStateException("Field not found. fieldName=" + fieldName);
    }
    ReflectionUtils.makeAccessible(field);
    ReflectionUtils.setField(field, target, value);
  }
}
