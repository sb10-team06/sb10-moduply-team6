package com.team6.moduply.content.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventResponse;
import com.team6.moduply.content.external.tmdb.TmdbProperties;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExternalContentMapperTest {

  private final ExternalContentMapper mapper = new ExternalContentMapper(createTmdbProperties());

  @Test
  @DisplayName("TMDB 영화 응답을 외부 콘텐츠 저장 모델로 변환한다.")
  void toItem_success_with_tmdb_movie_response() {
    // Given
    TmdbMovieResponse response = new TmdbMovieResponse(
        27205L,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "/poster.jpg",
        "/backdrop.jpg",
        "2010-07-15"
    );

    // When
    ExternalContentItem item = mapper.toItem(response);

    // Then
    assertThat(item.externalApiId()).isEqualTo("tmdb:movie:27205");
    assertThat(item.type()).isEqualTo(ContentType.movie);
    assertThat(item.title()).isEqualTo("Inception");
    assertThat(item.description()).isEqualTo("꿈과 현실을 넘나드는 SF 영화");
    assertThat(item.thumbnailUrl())
        .isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
    assertThat(item.tags()).containsExactly("TMDB", "movie");
  }

  @Test
  @DisplayName("TMDB TV 응답 설명이 비어 있으면 제목을 설명으로 사용한다.")
  void toItem_success_with_tmdb_tv_response_without_overview() {
    // Given
    TmdbTvResponse response = new TmdbTvResponse(
        1399L,
        "Game of Thrones",
        " ",
        "/poster.jpg",
        "/backdrop.jpg",
        "2011-04-17"
    );

    // When
    ExternalContentItem item = mapper.toItem(response);

    // Then
    assertThat(item.externalApiId()).isEqualTo("tmdb:tv:1399");
    assertThat(item.type()).isEqualTo(ContentType.tvSeries);
    assertThat(item.title()).isEqualTo("Game of Thrones");
    assertThat(item.description()).isEqualTo("Game of Thrones");
    assertThat(item.thumbnailUrl())
        .isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
    assertThat(item.tags()).containsExactly("TMDB", "tvSeries");
  }

  @Test
  @DisplayName("TMDB 응답에 포스터가 없으면 배경 이미지로 썸네일 URL을 생성한다.")
  void toItem_success_with_tmdb_backdrop_when_poster_absent() {
    // Given
    TmdbMovieResponse response = new TmdbMovieResponse(
        27205L,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        null,
        "/backdrop.jpg",
        "2010-07-15"
    );

    // When
    ExternalContentItem item = mapper.toItem(response);

    // Then
    assertThat(item.thumbnailUrl())
        .isEqualTo("https://image.tmdb.org/t/p/w500/backdrop.jpg");
  }

  @Test
  @DisplayName("The Sports DB 경기 응답 설명이 비어 있으면 경기 정보로 설명을 생성한다.")
  void toItem_success_with_sportsdb_event_response_without_description() {
    // Given
    SportsDbEventResponse response = new SportsDbEventResponse(
        "2494000",
        "Arsenal vs Coventry City",
        "",
        "",
        "Soccer",
        "English Premier League",
        "2026-08-21"
    );

    // When
    ExternalContentItem item = mapper.toItem(response);

    // Then
    assertThat(item.externalApiId()).isEqualTo("sportsdb:event:2494000");
    assertThat(item.type()).isEqualTo(ContentType.sport);
    assertThat(item.title()).isEqualTo("Arsenal vs Coventry City");
    assertThat(item.description())
        .isEqualTo("English Premier League 2026-08-21 Arsenal vs Coventry City");
    assertThat(item.thumbnailUrl()).isNull();
    assertThat(item.tags()).containsExactly("SportsDB", "Soccer", "English Premier League");
  }

  @Test
  @DisplayName("The Sports DB 경기 일자가 비어 있으면 리그와 경기명으로 설명을 생성한다.")
  void toItem_success_with_sportsdb_event_response_without_event_date() {
    // Given
    SportsDbEventResponse response = new SportsDbEventResponse(
        "2494000",
        "Arsenal vs Coventry City",
        "",
        "",
        "Soccer",
        "English Premier League",
        " "
    );

    // When
    ExternalContentItem item = mapper.toItem(response);

    // Then
    assertThat(item.description())
        .isEqualTo("English Premier League Arsenal vs Coventry City");
  }

  @Test
  @DisplayName("The Sports DB 경기 응답의 썸네일 URL을 외부 콘텐츠 저장 모델에 포함한다.")
  void toItem_success_with_sportsdb_thumbnail_url() {
    // Given
    SportsDbEventResponse response = new SportsDbEventResponse(
        "2388152",
        "Houston Astros vs Minnesota Twins",
        "",
        "https://r2.thesportsdb.com/images/media/event/thumb/4gwoi61780299876.jpg",
        "Baseball",
        "MLB",
        "2026-07-01"
    );

    // When
    ExternalContentItem item = mapper.toItem(response);

    // Then
    assertThat(item.thumbnailUrl())
        .isEqualTo("https://r2.thesportsdb.com/images/media/event/thumb/4gwoi61780299876.jpg");
  }

  @Test
  @DisplayName("외부 응답 필수 값이 누락되면 변환에 실패한다.")
  void toItem_fail_when_required_field_is_missing() {
    // Given
    TmdbMovieResponse response = new TmdbMovieResponse(
        null,
        "Inception",
        "overview",
        null,
        null,
        null
    );

    // When & Then
    assertThatThrownBy(() -> mapper.toItem(response))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode())
              .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE);
          assertThat(exception.getDetails().get("source")).isEqualTo("TMDB");
        });
  }

  private static TmdbProperties createTmdbProperties() {
    TmdbProperties properties = new TmdbProperties();
    properties.setImageBaseUrl("https://image.tmdb.org/t/p");
    return properties;
  }
}
