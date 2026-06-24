package com.team6.moduply.content.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.content.enums.ContentType;
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

class ContentCreateRequestTest {

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
  @DisplayName("유효한 콘텐츠 생성이면 요청 검증 성공")
  void content_create_request_validation_success() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of("SF", "액션")
    );

    // When
    Set<ConstraintViolation<ContentCreateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("태그 목록이 비어 있거나 생략된 콘텐츠 생성 요청이면 검증 성공")
  void content_create_request_validation_success_without_tags() {
    // Given
    ContentCreateRequest emptyTagsRequest = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        List.of()
    );
    ContentCreateRequest nullTagsRequest = new ContentCreateRequest(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null
    );

    // When
    Set<ConstraintViolation<ContentCreateRequest>> emptyTagsViolations = validator.validate(
        emptyTagsRequest);
    Set<ConstraintViolation<ContentCreateRequest>> nullTagsViolations = validator.validate(
        nullTagsRequest);

    // Then
    assertThat(emptyTagsViolations).isEmpty();
    assertThat(nullTagsViolations).isEmpty();
  }

  @Test
  @DisplayName("필수 값이 없거나 비어 있는 콘텐츠 생성이면 요청 검증 실패")
  void content_create_request_validation_fail_when_required_fields_are_missing() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        null,
        " ",
        "",
        List.of()
    );

    // When
    Set<ConstraintViolation<ContentCreateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("type", "title", "description");
  }

  @Test
  @DisplayName("제목이 255자를 초과하면 콘텐츠 생성 요청 검증 실패")
  void content_create_request_validation_fail_when_title_exceeds_max_length() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.movie,
        "a".repeat(256),
        "꿈과 현실을 넘나드는 SF 영화",
        List.of()
    );

    // When
    Set<ConstraintViolation<ContentCreateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("title");
  }

  @Test
  @DisplayName("태그 이름이 비어 있거나 100자를 초과하면 콘텐츠 생성 요청 검증 실패")
  void content_create_request_validation_fail_when_tag_is_invalid() {
    // Given
    ContentCreateRequest request = new ContentCreateRequest(
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠",
        List.of(" ", "a".repeat(101))
    );

    // When
    Set<ConstraintViolation<ContentCreateRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("tags[0].<list element>", "tags[1].<list element>");
  }
}
