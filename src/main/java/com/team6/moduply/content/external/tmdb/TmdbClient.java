package com.team6.moduply.content.external.tmdb;

import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbPageResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class TmdbClient {

  private static final String DEFAULT_LANGUAGE = "ko-KR";

  private final RestClient restClient;

  private final TmdbProperties properties;

  public TmdbClient(
      @Qualifier("tmdbRestClient") RestClient restClient,
      TmdbProperties properties
  ) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public TmdbPageResponse<TmdbMovieResponse> fetchPopularMovies(int page) {
    return fetchPopularMovies(page, DEFAULT_LANGUAGE);
  }

  public TmdbPageResponse<TmdbMovieResponse> fetchPopularMovies(int page, String language) {
    validateReadAccessToken();

    TmdbPageResponse<TmdbMovieResponse> response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/movie/popular")
            .queryParam("language", language)
            .queryParam("page", page)
            .build())
        .header(HttpHeaders.AUTHORIZATION, bearerToken())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {
        });

    return Objects.requireNonNull(response, "TMDB 영화 목록 응답이 비어 있습니다.");
  }

  public TmdbPageResponse<TmdbTvResponse> fetchPopularTvSeries(int page) {
    return fetchPopularTvSeries(page, DEFAULT_LANGUAGE);
  }

  public TmdbPageResponse<TmdbTvResponse> fetchPopularTvSeries(int page, String language) {
    validateReadAccessToken();

    TmdbPageResponse<TmdbTvResponse> response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/tv/popular")
            .queryParam("language", language)
            .queryParam("page", page)
            .build())
        .header(HttpHeaders.AUTHORIZATION, bearerToken())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {
        });

    return Objects.requireNonNull(response, "TMDB TV 목록 응답이 비어 있습니다.");
  }

  public String buildImageUrl(String imagePath, String size) {
    if (!StringUtils.hasText(imagePath)) {
      return null;
    }

    String normalizedPath = imagePath.startsWith("/") ? imagePath : "/" + imagePath;

    return "%s/%s%s".formatted(
        properties.getImageBaseUrl(),
        size,
        normalizedPath
    );
  }

  private String bearerToken() {
    return "Bearer " + properties.getReadAccessToken();
  }

  private void validateReadAccessToken() {
    if (!StringUtils.hasText(properties.getReadAccessToken())) {
      throw new IllegalStateException("TMDB_READ_ACCESS_TOKEN 설정이 누락되었습니다.");
    }
  }
}
