package com.team6.moduply.content.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.enums.ContentSortBy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentFindAllRequestTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    validatorFactory.close();
  }

  @Test
  @DisplayName("콘텐츠 목록 조회 요청 값이 유효하면 검증에 성공한다.")
  void validate_success_with_valid_request() {
    // Given
    ContentFindAllRequest request = new ContentFindAllRequest(
        null,
        "dream",
        List.of("SF", "액션"),
        "2026-06-26T00:00:00Z",
        UUID.randomUUID(),
        20,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // When
    Set<ConstraintViolation<ContentFindAllRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("콘텐츠 목록 조회 요청의 기본값이 없으면 기본값을 적용한다.")
  void create_success_with_default_values() {
    // Given & When
    ContentFindAllRequest request = new ContentFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    // Then
    assertThat(request.limit()).isEqualTo(20);
    assertThat(request.sortBy()).isEqualTo(ContentSortBy.createdAt);
    assertThat(request.sortDirection()).isEqualTo(SortDirection.DESCENDING);
  }

  @Test
  @DisplayName("콘텐츠 목록 조회 요청의 페이지 크기가 범위를 벗어나면 검증에 실패한다.")
  void validate_fail_when_limit_is_out_of_range() {
    // Given
    ContentFindAllRequest lessThanMinRequest = new ContentFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        0,
        null,
        null
    );
    ContentFindAllRequest greaterThanMaxRequest = new ContentFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        101,
        null,
        null
    );

    // When
    Set<ConstraintViolation<ContentFindAllRequest>> lessThanMinViolations = validator.validate(
        lessThanMinRequest);
    Set<ConstraintViolation<ContentFindAllRequest>> greaterThanMaxViolations = validator.validate(
        greaterThanMaxRequest);

    // Then
    assertThat(lessThanMinViolations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("limit");
    assertThat(greaterThanMaxViolations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("limit");
  }

  @Test
  @DisplayName("콘텐츠 목록 조회 요청의 태그 이름이 비어 있거나 100자를 초과하면 검증에 실패한다.")
  void validate_fail_when_tag_is_invalid() {
    // Given
    ContentFindAllRequest request = new ContentFindAllRequest(
        null,
        null,
        List.of(" ", "a".repeat(101)),
        null,
        null,
        null,
        null,
        null
    );

    // When
    Set<ConstraintViolation<ContentFindAllRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("tagsIn[0].<list element>", "tagsIn[1].<list element>");
  }

  @Test
  @DisplayName("콘텐츠 목록 조회 요청의 커서와 보조 커서 중 하나만 있으면 검증에 실패한다.")
  void validate_fail_when_cursor_pair_is_incomplete() {
    // Given
    ContentFindAllRequest cursorOnlyRequest = new ContentFindAllRequest(
        null,
        null,
        null,
        "2026-06-26T00:00:00Z",
        null,
        null,
        null,
        null
    );
    ContentFindAllRequest idAfterOnlyRequest = new ContentFindAllRequest(
        null,
        null,
        null,
        null,
        UUID.randomUUID(),
        null,
        null,
        null
    );

    // When
    Set<ConstraintViolation<ContentFindAllRequest>> cursorOnlyViolations = validator.validate(
        cursorOnlyRequest);
    Set<ConstraintViolation<ContentFindAllRequest>> idAfterOnlyViolations = validator.validate(
        idAfterOnlyRequest);

    // Then
    assertThat(cursorOnlyViolations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("cursorPairValid");
    assertThat(idAfterOnlyViolations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("cursorPairValid");
  }
}
