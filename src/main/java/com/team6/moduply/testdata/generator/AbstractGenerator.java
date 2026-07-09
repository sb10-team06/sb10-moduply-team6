package com.team6.moduply.testdata.generator;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.team6.moduply.common.baseentity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 1. 이 클래스를 상속받아 T(엔티티)를 지정합니다.
 * <p>
 * 2. getModel(): Instancio를 사용하여 엔티티 필드 생성 규칙을 정의합니다.
 * <p>
 * 3. getSql(): JDBC용 INSERT 쿼리를 작성합니다. 4. setValues(): PreparedStatement에 엔티티 값을 매핑합니다.
 * <p>
 * 그외
 * <p>
 * - @Profile("data-gen")을 구현 클래스에 붙여주세요.
 * <p>
 * - @Qualifier("dataGeneratorExecutor")를 생성자에 주입받아야 합니다.
 * <p>
 * - 성능을 위해 JDBC batchUpdate를 사용합니다.
 * <p>
 * - 날짜를 사용하는 경우, 데이터 특성을 고려하여 정규 분포랑 균등 분포 중 선택하여 사용해주세요.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGenerator<T extends BaseEntity> {       // T에 User, Follow, Content등을 넣어 어떤 Enitity든 사용할 수 있게 셋팅

  protected final JdbcTemplate jdbcTemplate;                          // DB에 Batch Insert 하기위해 사용
  protected final Executor dataGeneratorExecutor;                     // 병렬 실행용 Thread Pool

  private static final ThreadLocal<Faker> THREAD_LOCAL_FAKER =        // 병렬로 처리되는 Thread마다 Faker 하나씩 가지도록 ThreadLocal 사용
          ThreadLocal.withInitial(Faker::new);

  protected Faker faker() {
    return THREAD_LOCAL_FAKER.get();
  }

  // 날짜 유형
  // 1. 균등 분포 날짜: 모든 날짜가 골고루(ex.가입일)
  // 원하는 일수(ex.30일) 기준 균등
  protected Timestamp getUniformTimestamp(int daysBack) {
    long maxSeconds = (long) daysBack * 24 * 60 * 60;
    long randomSeconds = ThreadLocalRandom.current().nextLong(maxSeconds);
    return Timestamp.from(Instant.now().minusSeconds(randomSeconds));
  }

  // 2. 정규 분포 날짜: 최근 날짜에 데이터가 밀집될 때 (ex.최근 활동)
  protected Timestamp getNormalTimestamp(int daysBack) {
    long maxSeconds = (long) daysBack * 24 * 60 * 60;
    // 평균을 0(현재)에 가깝게 두고 표준편차를 적용하여 최근에 가중치 부여
    double gaussian = ThreadLocalRandom.current().nextGaussian();
    long offset = (long) (gaussian * (maxSeconds / 3)); // 최근에 쏠리도록 범위 조절

    // 너무 과거로 가지 않게 Clamp 처리
    long finalSeconds = Math.max(0, Math.min(maxSeconds, offset));
    return Timestamp.from(Instant.now().minusSeconds(finalSeconds));
  }

  // 구현 메서드
  protected abstract Model<T> getModel();         // Instancio 모델 반환

  protected abstract String getSql();

  protected abstract void setValues(PreparedStatement ps, T entity) throws SQLException;

  // 병렬로 데이터 생성 -> 배치로 저장
  public List<T> generate(int totalSize, int chunkSize) {
    int numTasks = (int) Math.ceil((double) totalSize / chunkSize);
    AtomicInteger insertedCount = new AtomicInteger(0); // 삽입 성공 건수 추적

    ConcurrentLinkedQueue<T> allGeneratedEntities = new ConcurrentLinkedQueue<>();
    List<CompletableFuture<Void>> futures = IntStream.range(0, numTasks)
            .mapToObj(i -> {
              int currentChunkSize = Math.min(chunkSize, totalSize - (i * chunkSize));
              return CompletableFuture.runAsync(() -> {
                        // 청크 사이즈만큼 생성
                        List<T> chunk = Instancio.ofList(getModel()).size(currentChunkSize).create();
                        executeBatch(chunk);
                        allGeneratedEntities.addAll(chunk);
                        insertedCount.addAndGet(chunk.size());
                      }, dataGeneratorExecutor)
                      .exceptionally(ex -> {
                        log.error("청크 {}번 삽입 중 오류 발생", i, ex);
                        return null;
                      });
            })
            .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    int inserted = insertedCount.get();
    if (inserted < totalSize) {
      log.warn("데이터 생성 부분 실패: {}건 삽입 성공, {}건 실패 (요청: {}건)",
              inserted, totalSize - inserted, totalSize);
    } else {
      log.info("데이터 생성 작업 완료: 총 {}건 삽입 (요청: {}건)", inserted, totalSize);
    }
    return new ArrayList<>(allGeneratedEntities);
  }

  protected void executeBatch(List<T> entities) {
    jdbcTemplate.batchUpdate(getSql(), entities, entities.size(), this::setValues);
  }
}
