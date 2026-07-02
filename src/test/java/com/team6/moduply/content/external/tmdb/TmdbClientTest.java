package com.team6.moduply.content.external.tmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbPageResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TmdbClientTest {

  private MockRestServiceServer server;

  private TmdbClient tmdbClient;

  private TmdbProperties properties;

  @BeforeEach
  void setUp() {
    properties = new TmdbProperties();
    properties.setBaseUrl("https://api.themoviedb.org/3");
    properties.setImageBaseUrl("https://image.tmdb.org/t/p");
    properties.setReadAccessToken("test-token");

    RestClient.Builder builder = RestClient.builder()
        .baseUrl(properties.getBaseUrl());
    server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    tmdbClient = new TmdbClient(restClient, properties);
  }

  @Test
  @DisplayName("TMDB 인기 영화 목록 조회에 성공한다.")
  void fetchPopularMovies_success_with_valid_request() {
    // Given
    String json = """
        {
          "page": 1,
          "results": [
            {
              "id": 27205,
              "title": "Inception",
              "overview": "꿈과 현실을 넘나드는 SF 영화",
              "poster_path": "/inception-poster.jpg",
              "backdrop_path": "/inception-backdrop.jpg",
              "release_date": "2010-07-15"
            }
          ],
          "total_pages": 500,
          "total_results": 10000
        }
        """;
    server.expect(once(), requestTo(
            "https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    // When
    TmdbPageResponse<TmdbMovieResponse> response = tmdbClient.fetchPopularMovies(1);

    // Then
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(500);
    assertThat(response.totalResults()).isEqualTo(10000);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).title()).isEqualTo("Inception");
    server.verify();
  }

  @Test
  @DisplayName("TMDB 인기 TV 목록 조회에 성공한다.")
  void fetchPopularTvSeries_success_with_valid_request() {
    // Given
    String json = """
        {
          "page": 1,
          "results": [
            {
              "id": 1399,
              "name": "Game of Thrones",
              "overview": "Seven noble families fight for control.",
              "poster_path": "/got-poster.jpg",
              "backdrop_path": "/got-backdrop.jpg",
              "first_air_date": "2011-04-17"
            }
          ],
          "total_pages": 100,
          "total_results": 2000
        }
        """;
    server.expect(once(), requestTo(
            "https://api.themoviedb.org/3/tv/popular?language=en-US&page=2"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    // When
    TmdbPageResponse<TmdbTvResponse> response = tmdbClient.fetchPopularTvSeries(2, "en-US");

    // Then
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(100);
    assertThat(response.totalResults()).isEqualTo(2000);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).name()).isEqualTo("Game of Thrones");
    server.verify();
  }

  @Test
  @DisplayName("TMDB 이미지 경로와 크기를 조합해 이미지 URL 생성에 성공한다.")
  void buildImageUrl_success_with_image_path_and_size() {
    // Given
    String imagePath = "/poster.jpg";
    String size = "w500";

    // When
    String imageUrl = tmdbClient.buildImageUrl(imagePath, size);

    // Then
    assertThat(imageUrl).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
  }

  @Test
  @DisplayName("TMDB 이미지 경로가 비어 있으면 이미지 URL 생성 결과로 null을 반환한다.")
  void buildImageUrl_success_with_blank_image_path() {
    // Given
    String imagePath = " ";

    // When
    String imageUrl = tmdbClient.buildImageUrl(imagePath, "w500");

    // Then
    assertThat(imageUrl).isNull();
  }

  @Test
  @DisplayName("TMDB 읽기 액세스 토큰이 없으면 인기 영화 목록 조회에 실패한다.")
  void fetchPopularMovies_fail_when_read_access_token_is_blank() {
    // Given
    properties.setReadAccessToken(" ");

    // When & Then
    assertThatThrownBy(() -> tmdbClient.fetchPopularMovies(1))
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_CONFIG_INVALID)
        );
  }

  @Test
  @DisplayName("TMDB API 오류 응답이 발생하면 ContentException으로 변환한다.")
  void fetchPopularMovies_fail_when_external_api_error_occurs() {
    // Given
    server.expect(once(), requestTo(
            "https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
        .andRespond(withServerError());

    // When & Then
    assertThatThrownBy(() -> tmdbClient.fetchPopularMovies(1))
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE)
        );

    server.verify();
  }
}
