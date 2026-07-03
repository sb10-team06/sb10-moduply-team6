package com.team6.moduply.content.external.tmdb;

import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbPageResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
    return fetchPopular(
        "/movie/popular",
        page,
        language,
        new ParameterizedTypeReference<>() {
        },
        "TMDB 영화 목록 응답이 비어 있습니다."
    );
  }

  public TmdbPageResponse<TmdbTvResponse> fetchPopularTvSeries(int page) {
    return fetchPopularTvSeries(page, DEFAULT_LANGUAGE);
  }

  public TmdbPageResponse<TmdbTvResponse> fetchPopularTvSeries(int page, String language) {
    return fetchPopular(
        "/tv/popular",
        page,
        language,
        new ParameterizedTypeReference<>() {
        },
        "TMDB TV 목록 응답이 비어 있습니다."
    );
  }

  private <T> TmdbPageResponse<T> fetchPopular(
      String path,
      int page,
      String language,
      ParameterizedTypeReference<TmdbPageResponse<T>> responseType,
      String emptyMessage
  ) {
    validateReadAccessToken();

    try {
      TmdbPageResponse<T> response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path(path)
              .queryParam("language", language)
              .queryParam("page", page)
              .build())
          .header(HttpHeaders.AUTHORIZATION, bearerToken())
          .retrieve()
          .body(responseType);

      return requireResponse(response, emptyMessage, path);
    } catch (RestClientException | IllegalArgumentException e) {
      throw new ContentException(
          ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE,
          Map.of("provider", "TMDB", "path", path),
          e
      );
    }
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
      throw new ContentException(
          ContentErrorCode.EXTERNAL_CONTENT_CONFIG_INVALID,
          Map.of("property", "TMDB_READ_ACCESS_TOKEN")
      );
    }
  }

  private <T> TmdbPageResponse<T> requireResponse(
      TmdbPageResponse<T> response,
      String message,
      String path
  ) {
    if (response == null) {
      throw new ContentException(
          ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE,
          Map.of("provider", "TMDB", "path", path, "reason", message)
      );
    }

    return response;
  }
}
