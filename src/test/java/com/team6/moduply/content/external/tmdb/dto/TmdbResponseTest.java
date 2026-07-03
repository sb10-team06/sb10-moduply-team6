package com.team6.moduply.content.external.tmdb.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TmdbResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @DisplayName("TMDB 영화 목록 응답을 역직렬화하면 필요한 필드가 유지된다.")
  void deserialize_success_with_movie_page_response() throws Exception {
    // Given
    String json = """
        {
          "page": 1,
          "results": [
            {
              "id": 27205,
              "title": "Inception",
              "overview": "A thief who steals corporate secrets through dream-sharing technology.",
              "poster_path": "/inception-poster.jpg",
              "backdrop_path": "/inception-backdrop.jpg",
              "release_date": "2010-07-15",
              "vote_average": 8.4
            }
          ],
          "total_pages": 500,
          "total_results": 10000
        }
        """;

    // When
    TmdbPageResponse<TmdbMovieResponse> response = objectMapper.readValue(
        json,
        new TypeReference<>() {
        }
    );

    // Then
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(500);
    assertThat(response.totalResults()).isEqualTo(10000);
    assertThat(response.results()).hasSize(1);

    TmdbMovieResponse movie = response.results().get(0);
    assertThat(movie.id()).isEqualTo(27205L);
    assertThat(movie.title()).isEqualTo("Inception");
    assertThat(movie.overview()).startsWith("A thief");
    assertThat(movie.posterPath()).isEqualTo("/inception-poster.jpg");
    assertThat(movie.backdropPath()).isEqualTo("/inception-backdrop.jpg");
    assertThat(movie.releaseDate()).isEqualTo("2010-07-15");
  }

  @Test
  @DisplayName("TMDB TV 목록 응답을 역직렬화하면 필요한 필드가 유지된다.")
  void deserialize_success_with_tv_page_response() throws Exception {
    // Given
    String json = """
        {
          "page": 1,
          "results": [
            {
              "id": 1399,
              "name": "Game of Thrones",
              "overview": "Seven noble families fight for control of the mythical land of Westeros.",
              "poster_path": "/got-poster.jpg",
              "backdrop_path": "/got-backdrop.jpg",
              "first_air_date": "2011-04-17",
              "origin_country": ["US"]
            }
          ],
          "total_pages": 100,
          "total_results": 2000
        }
        """;

    // When
    TmdbPageResponse<TmdbTvResponse> response = objectMapper.readValue(
        json,
        new TypeReference<>() {
        }
    );

    // Then
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(100);
    assertThat(response.totalResults()).isEqualTo(2000);
    assertThat(response.results()).hasSize(1);

    TmdbTvResponse tv = response.results().get(0);
    assertThat(tv.id()).isEqualTo(1399L);
    assertThat(tv.name()).isEqualTo("Game of Thrones");
    assertThat(tv.overview()).startsWith("Seven noble families");
    assertThat(tv.posterPath()).isEqualTo("/got-poster.jpg");
    assertThat(tv.backdropPath()).isEqualTo("/got-backdrop.jpg");
    assertThat(tv.firstAirDate()).isEqualTo("2011-04-17");
  }
}
