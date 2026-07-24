package com.team6.moduply.testdata.generator;

import com.team6.moduply.testdata.ReviewTestDataProperties;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewGeneratorTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private TransactionTemplate transactionTemplate;

  private ReviewTestDataProperties properties;
  private ReviewGenerator reviewGenerator;

  @BeforeEach
  void setUp() {
    properties = new ReviewTestDataProperties();
    properties.setEnabled(true);
    properties.setHotContentSize(2);
    properties.setReviewsPerContent(2);
    properties.setChunkSize(10);
    properties.setSkipIfExists(true);

    reviewGenerator = new ReviewGenerator(jdbcTemplate, transactionTemplate, properties);
  }

  @Test
  @DisplayName("부분 생성된 리뷰가 있으면 누락된 리뷰만 추가하고 콘텐츠 통계를 갱신한다.")
  void generate_success_when_partial_reviews_exist() {
    // given
    UUID content1Id = UUID.randomUUID();
    UUID content2Id = UUID.randomUUID();
    UUID author1Id = UUID.randomUUID();
    UUID author2Id = UUID.randomUUID();

    given(jdbcTemplate.query(anyString(), anyUuidRowMapper(), any(), any()))
        .willReturn(
            List.of(content1Id, content2Id),
            List.of(author1Id, author2Id),
            List.of(author1Id),
            List.of()
        );
    given(transactionTemplate.execute(any()))
        .willAnswer(invocation -> {
          TransactionCallback<?> callback = invocation.getArgument(0);
          return callback.doInTransaction(null);
        });
    doAnswer(invocation -> {
      Consumer<TransactionStatus> callback = invocation.getArgument(0);
      callback.accept(null);
      return null;
    }).when(transactionTemplate).executeWithoutResult(any());
    AtomicInteger batchReviewCount = new AtomicInteger();
    AtomicInteger refreshStatsCount = new AtomicInteger();
    given(jdbcTemplate.batchUpdate(anyString(), any(Collection.class), anyInt(), any()))
        .willAnswer(invocation -> {
          Collection<?> reviews = invocation.getArgument(1);
          batchReviewCount.set(reviews.size());
          int[] results = new int[reviews.size()];
          for (int i = 0; i < results.length; i += 1) {
            results[i] = 1;
          }
          return new int[][]{results};
        });
    given(jdbcTemplate.update(anyString(), any(Timestamp.class), any(UUID.class)))
        .willAnswer(invocation -> {
          refreshStatsCount.incrementAndGet();
          return 1;
        });

    // when
    reviewGenerator.generate();

    // then
    assertThat(batchReviewCount).hasValue(3);
    assertThat(refreshStatsCount).hasValue(2);
  }

  @Test
  @DisplayName("선택된 콘텐츠와 작성자 조합이 모두 존재하면 리뷰 생성을 건너뛴다.")
  void generate_success_when_generated_reviews_are_complete() {
    // given
    UUID content1Id = UUID.randomUUID();
    UUID content2Id = UUID.randomUUID();
    UUID author1Id = UUID.randomUUID();
    UUID author2Id = UUID.randomUUID();
    List<UUID> authorIds = List.of(author1Id, author2Id);

    given(jdbcTemplate.query(anyString(), anyUuidRowMapper(), any(), any()))
        .willReturn(
            List.of(content1Id, content2Id),
            authorIds,
            authorIds,
            authorIds
        );

    // when
    reviewGenerator.generate();

    // then
    verify(jdbcTemplate, never()).batchUpdate(anyString(), any(Collection.class), anyInt(), any());
    verify(transactionTemplate, never()).executeWithoutResult(any());
  }

  @SuppressWarnings("unchecked")
  private RowMapper<UUID> anyUuidRowMapper() {
    return any(RowMapper.class);
  }
}
