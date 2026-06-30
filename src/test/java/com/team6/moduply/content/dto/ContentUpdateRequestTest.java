package com.team6.moduply.content.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentUpdateRequestTest {

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
  @DisplayName("유효한 콘텐츠 수정 요청이면 검증에 성공한다.")
  void validate_success_with_valid_request() {
    // Given
    ContentUpdateRequest request = new ContentUpdateRequest(
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );

    // When
    Set<ConstraintViolation<ContentUpdateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("일부 필드만 포함한 콘텐츠 수정 요청이면 검증에 성공한다.")
  void validate_success_with_partial_request() {
    // Given
    ContentUpdateRequest titleOnlyRequest = new ContentUpdateRequest(
        "Inception",
        null,
        null
    );
    ContentUpdateRequest tagsOnlyRequest = new ContentUpdateRequest(
        null,
        null,
        List.of("SF", "액션")
    );

    // When
    Set<ConstraintViolation<ContentUpdateRequest>> titleOnlyViolations = validator.validate(
        titleOnlyRequest);
    Set<ConstraintViolation<ContentUpdateRequest>> tagsOnlyViolations = validator.validate(
        tagsOnlyRequest);

    // Then
    assertThat(titleOnlyViolations).isEmpty();
    assertThat(tagsOnlyViolations).isEmpty();
  }

  @Test
  @DisplayName("태그 목록이 비어 있거나 생략된 콘텐츠 수정 요청이면 검증에 성공한다.")
  void validate_success_without_tags() {
    // Given
    ContentUpdateRequest emptyTagsRequest = new ContentUpdateRequest(
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of()
    );
    ContentUpdateRequest nullTagsRequest = new ContentUpdateRequest(
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null
    );

    // When
    Set<ConstraintViolation<ContentUpdateRequest>> emptyTagsViolations = validator.validate(
        emptyTagsRequest);
    Set<ConstraintViolation<ContentUpdateRequest>> nullTagsViolations = validator.validate(
        nullTagsRequest);

    // Then
    assertThat(emptyTagsViolations).isEmpty();
    assertThat(nullTagsViolations).isEmpty();
  }

  @Test
  @DisplayName("모든 필드를 생략한 콘텐츠 수정 요청이면 검증에 성공한다.")
  void validate_success_with_empty_request() {
    // Given
    ContentUpdateRequest request = new ContentUpdateRequest(
        null,
        null,
        null
    );

    // When
    Set<ConstraintViolation<ContentUpdateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("제목이 255자를 초과하면 콘텐츠 수정 요청 검증에 실패한다.")
  void validate_fail_when_title_exceeds_max_length() {
    // Given
    ContentUpdateRequest request = new ContentUpdateRequest(
        "a".repeat(256),
        "꿈과 현실을 넘나드는 SF 영화",
        List.of()
    );

    // When
    Set<ConstraintViolation<ContentUpdateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("title");
  }

  @Test
  @DisplayName("제목이 공백만 있으면 콘텐츠 수정 요청 검증에 실패한다.")
  void validate_fail_when_title_is_blank() {
    // Given
    ContentUpdateRequest request = new ContentUpdateRequest(
        "   ",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of()
    );

    // When
    Set<ConstraintViolation<ContentUpdateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("title");
  }

  @Test
  @DisplayName("태그 이름이 비어 있거나 100자를 초과하면 콘텐츠 수정 요청 검증에 실패한다.")
  void validate_fail_when_tag_is_invalid() {
    // Given
    ContentUpdateRequest request = new ContentUpdateRequest(
        "World Cup",
        "스포츠 콘텐츠",
        List.of(" ", "a".repeat(101))
    );

    // When
    Set<ConstraintViolation<ContentUpdateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("tags[0].<list element>", "tags[1].<list element>");
  }
}
