package com.team6.moduply.testdata.generator;

import com.team6.moduply.common.baseentity.BaseEntity;
import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import com.team6.moduply.testdata.UserTestDataProperties;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Component
@Profile("data-gen")
public class UserGenerator extends AbstractGenerator<User> {

  public static final String FOLLOWER_EMAIL_PREFIX = "k6-follower-";
  public static final String FOLLOWEE_EMAIL_PREFIX = "k6-followee-";
  public static final String CONVERSATION_EMAIL_PREFIX = "k6-conversation-";
  public static final String PROFILE_UPDATE_EMAIL_PREFIX = "k6-user-";
  public static final String REVIEW_EMAIL_PREFIX = "k6-review-";
  public static final String EMAIL_DOMAIN = "@moduply.test";

  private static final String INSERT_USER_SQL = """
      insert into users (
        id, profile_img_id, email, password, name, role, is_blocked, created_at, updated_at
      ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private final UserTestDataProperties properties;
  private final PasswordEncoder passwordEncoder;
  private final AtomicInteger sequence = new AtomicInteger();

  private String currentEmailPrefix = FOLLOWER_EMAIL_PREFIX;
  private String currentNamePrefix = "k6 follower";
  private String encodedPassword;

  public UserGenerator(
      JdbcTemplate jdbcTemplate,
      Executor dataGeneratorExecutor,
      UserTestDataProperties properties,
      PasswordEncoder passwordEncoder
  ) {
    super(jdbcTemplate, dataGeneratorExecutor);
    this.properties = properties;
    this.passwordEncoder = passwordEncoder;
  }

  public void generate() {
    if (!properties.isEnabled()) {
      log.info("[UserGenerator] disabled. skip generation.");
      return;
    }

    encodedPassword = passwordEncoder.encode(properties.getPassword());
    log.info(
        "[UserGenerator] start generation. followerSize={}, followeeSize={}, conversationSize={}, profileUpdateSize={}, reviewSize={}, chunkSize={}",
        properties.getFollowerSize(),
        properties.getFolloweeSize(),
        properties.getConversationSize(),
        properties.getProfileUpdateSize(),
        properties.getReviewSize(),
        properties.getChunkSize()
    );

    generateGroup(FOLLOWER_EMAIL_PREFIX, "k6 follower", properties.getFollowerSize());
    generateGroup(FOLLOWEE_EMAIL_PREFIX, "k6 followee", properties.getFolloweeSize());
    generateGroup(CONVERSATION_EMAIL_PREFIX, "k6 conversation", properties.getConversationSize());
    generateGroup(PROFILE_UPDATE_EMAIL_PREFIX, "k6 user", properties.getProfileUpdateSize());
    generateGroup(REVIEW_EMAIL_PREFIX, "k6 review", properties.getReviewSize());
  }

  @Override
  public List<User> generate(int totalSize, int chunkSize) {
    preparePassword();
    return super.generate(totalSize, chunkSize);
  }

  @Override
  protected Model<User> getModel() {
    return Instancio.of(User.class)
        .supply(Select.field(User.class, "email"), this::createEmail)
        .supply(Select.field(User.class, "encodedPassword"), () -> encodedPassword)
        .supply(Select.field(User.class, "name"), this::createName)
        .supply(Select.field(User.class, "role"), () -> Role.USER)
        .set(Select.field(User.class, "profileImg"), null)
        .set(Select.field(User.class, "isBlocked"), false)
        .onComplete(Select.all(User.class), this::setBaseFields)
        .toModel();
  }

  @Override
  protected String getSql() {
    return INSERT_USER_SQL;
  }

  @Override
  protected void setValues(PreparedStatement ps, User user)
      throws SQLException {
    ps.setObject(1, user.getId());
    ps.setObject(2, null);
    ps.setString(3, user.getEmail());
    ps.setString(4, user.getEncodedPassword());
    ps.setString(5, user.getName());
    ps.setString(6, user.getRole().name());
    ps.setBoolean(7, user.isBlocked());
    ps.setTimestamp(8, Timestamp.from(user.getCreatedAt()));
    ps.setTimestamp(9, Timestamp.from(user.getUpdatedAt()));
  }

  private void generateGroup(String emailPrefix, String namePrefix, int size) {
    if (size <= 0) {
      return;
    }

    long existingCount = countGeneratedUsers(emailPrefix);
    if (properties.isSkipIfExists() && existingCount > 0) {
      log.info("[UserGenerator] generated users already exist. prefix={}, count={}",
          emailPrefix, existingCount);
      return;
    }

    currentEmailPrefix = emailPrefix;
    currentNamePrefix = namePrefix;
    sequence.set(0);
    generate(size, properties.getChunkSize());
  }

  private void preparePassword() {
    if (encodedPassword == null) {
      encodedPassword = passwordEncoder.encode(properties.getPassword());
    }
  }

  private long countGeneratedUsers(String emailPrefix) {
    Long count = jdbcTemplate.queryForObject(
        "select count(*) from users where email like ?",
        Long.class,
        emailPrefix + "%" + EMAIL_DOMAIN
    );
    return count == null ? 0 : count;
  }

  private String createEmail() {
    int value = sequence.incrementAndGet();
    return "%s%06d%s".formatted(currentEmailPrefix, value, EMAIL_DOMAIN);
  }

  private String createName() {
    return "%s %06d".formatted(currentNamePrefix, sequence.get());
  }

  private void setBaseFields(User user) {
    Instant createdAt = getUniformTimestamp(180).toInstant();
    setField(user, BaseEntity.class, "id", UUID.randomUUID());
    setField(user, BaseEntity.class, "createdAt", createdAt);
    setField(user, BaseUpdatableEntity.class, "updatedAt", createdAt);
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
