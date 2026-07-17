package com.team6.moduply.content.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentImportBatchPropertiesTest {

  @Test
  @DisplayName("TMDB 페이지 종료값은 시작값보다 크거나 같고 20 이하이면 유효하다.")
  void tmdb_page_range_success() {
    // Given
    ContentImportBatchProperties properties = new ContentImportBatchProperties();
    properties.setTmdbPageStart(1);
    properties.setTmdbPageEnd(20);
    properties.setInitialTmdbPageStart(1);
    properties.setInitialTmdbPageEnd(10);

    // When & Then
    assertThat(properties.isValidTmdbPageRange()).isTrue();
    assertThat(properties.isValidTmdbPageUpperBound()).isTrue();
    assertThat(properties.isValidInitialTmdbPageRange()).isTrue();
    assertThat(properties.isValidInitialTmdbPageUpperBound()).isTrue();
  }

  @Test
  @DisplayName("TMDB 페이지 종료값이 20을 초과하면 유효하지 않다.")
  void tmdb_page_range_fail_when_end_exceeds_upper_bound() {
    // Given
    ContentImportBatchProperties properties = new ContentImportBatchProperties();
    properties.setTmdbPageEnd(21);
    properties.setInitialTmdbPageEnd(21);

    // When & Then
    assertThat(properties.isValidTmdbPageUpperBound()).isFalse();
    assertThat(properties.isValidInitialTmdbPageUpperBound()).isFalse();
  }

  @Test
  @DisplayName("Sports DB 날짜 offset은 -30 이상 30 이하이고 최대 31일 범위이면 유효하다.")
  void sports_db_day_offset_range_success() {
    // Given
    ContentImportBatchProperties properties = new ContentImportBatchProperties();
    properties.setSportsDbDayOffsetStart(-3);
    properties.setSportsDbDayOffsetEnd(7);
    properties.setInitialSportsDbDayOffsetStart(-3);
    properties.setInitialSportsDbDayOffsetEnd(7);

    // When & Then
    assertThat(properties.isValidSportsDbDayOffsetBounds()).isTrue();
    assertThat(properties.isValidSportsDbDayOffsetRangeSize()).isTrue();
    assertThat(properties.isValidInitialSportsDbDayOffsetBounds()).isTrue();
    assertThat(properties.isValidInitialSportsDbDayOffsetRangeSize()).isTrue();
  }

  @Test
  @DisplayName("Sports DB 날짜 offset이 절대 상한을 벗어나거나 범위가 31일을 초과하면 유효하지 않다.")
  void sports_db_day_offset_range_fail_when_bounds_or_size_invalid() {
    // Given
    ContentImportBatchProperties properties = new ContentImportBatchProperties();
    properties.setSportsDbDayOffsetStart(-31);
    properties.setSportsDbDayOffsetEnd(1);
    properties.setInitialSportsDbDayOffsetStart(0);
    properties.setInitialSportsDbDayOffsetEnd(31);

    // When & Then
    assertThat(properties.isValidSportsDbDayOffsetBounds()).isFalse();
    assertThat(properties.isValidSportsDbDayOffsetRangeSize()).isFalse();
    assertThat(properties.isValidInitialSportsDbDayOffsetBounds()).isFalse();
    assertThat(properties.isValidInitialSportsDbDayOffsetRangeSize()).isFalse();
  }
}
