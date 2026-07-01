package com.team6.moduply.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.user.enums.UserSortBy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserFindAllRequestTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidatorFactory() {
    validatorFactory.close();
  }

  @Test
  @DisplayName("사용자 목록 조회 요청은 커서와 보조 커서가 모두 없으면 유효하다.")
  void validate_success_when_cursor_pair_absent() {
    // Given
    UserFindAllRequest request = new UserFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        20,
        SortDirection.ASCENDING,
        UserSortBy.name
    );

    // When
    Set<ConstraintViolation<UserFindAllRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("사용자 목록 조회 요청은 커서만 있으면 유효하지 않다.")
  void validate_fail_when_cursor_without_id_after() {
    // Given
    UserFindAllRequest request = new UserFindAllRequest(
        null,
        null,
        null,
        "tester@example.com",
        null,
        20,
        SortDirection.ASCENDING,
        UserSortBy.email
    );

    // When
    Set<ConstraintViolation<UserFindAllRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains("cursor와 idAfter는 함께 전달되어야 합니다.");
  }

  @Test
  @DisplayName("사용자 목록 조회 요청은 보조 커서만 있으면 유효하지 않다.")
  void validate_fail_when_id_after_without_cursor() {
    // Given
    UserFindAllRequest request = new UserFindAllRequest(
        null,
        null,
        null,
        null,
        UUID.randomUUID(),
        20,
        SortDirection.ASCENDING,
        UserSortBy.email
    );

    // When
    Set<ConstraintViolation<UserFindAllRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains("cursor와 idAfter는 함께 전달되어야 합니다.");
  }

  @Test
  @DisplayName("사용자 목록 조회 요청은 필수 정렬 조건이 없으면 유효하지 않다.")
  void validate_fail_when_required_sort_parameters_absent() {
    // Given
    UserFindAllRequest request = new UserFindAllRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    // When
    Set<ConstraintViolation<UserFindAllRequest>> violations = validator.validate(request);

    // Then
    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains(
            "limit은 필수입니다.",
            "sortDirection은 필수입니다.",
            "sortBy는 필수입니다."
        );
  }
}
