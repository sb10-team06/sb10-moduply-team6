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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

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

  public TmdbPageResponse<TmdbMovieResponse> fetchDiscoverMovies(int page) {
    return fetchDiscoverMovies(page, DEFAULT_LANGUAGE);
  }

  @Retryable(
      retryFor = ContentException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  public TmdbPageResponse<TmdbMovieResponse> fetchDiscoverMovies(int page, String language) {
    return fetchDiscover(
        "/discover/movie",
        page,
        language,
        this::applyMovieDiscoverParams,
        new ParameterizedTypeReference<>() {
        },
        "TMDB 영화 목록 응답이 비어 있습니다."
    );
  }

  public TmdbPageResponse<TmdbTvResponse> fetchDiscoverTvSeries(int page) {
    return fetchDiscoverTvSeries(page, DEFAULT_LANGUAGE);
  }

  @Retryable(
      retryFor = ContentException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  public TmdbPageResponse<TmdbTvResponse> fetchDiscoverTvSeries(int page, String language) {
    return fetchDiscover(
        "/discover/tv",
        page,
        language,
        this::applyTvDiscoverParams,
        new ParameterizedTypeReference<>() {
        },
        "TMDB TV 목록 응답이 비어 있습니다."
    );
  }

  private <T> TmdbPageResponse<T> fetchDiscover(
      String path,
      int page,
      String language,
      DiscoverParamCustomizer discoverParamCustomizer,
      ParameterizedTypeReference<TmdbPageResponse<T>> responseType,
      String emptyMessage
  ) {
    validateReadAccessToken();

    try {
      TmdbPageResponse<T> response = restClient.get()
          .uri(uriBuilder -> {
            UriBuilder builder = uriBuilder
              .path(path)
              .queryParam("language", language)
              .queryParam("page", page);
            discoverParamCustomizer.customize(builder);
            return builder.build();
          })
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

  private void applyMovieDiscoverParams(UriBuilder builder) {
    builder
        .queryParam("include_adult", properties.isMovieIncludeAdult())
        .queryParam("include_video", properties.isMovieIncludeVideo())
        .queryParam("sort_by", properties.getMovieSortBy());

    queryParamIfHasText(builder, "region", properties.getMovieRegion());
    queryParamIfHasText(builder, "certification_country", properties.getMovieCertificationCountry());
    queryParamIfHasText(builder, "certification.lte", properties.getMovieCertificationLte());
    queryParamIfHasText(builder, "vote_count.gte", properties.getMovieVoteCountGte());
    queryParamIfHasText(builder, "without_keywords", properties.getMovieWithoutKeywords());
    queryParamIfHasText(builder, "without_genres", properties.getMovieWithoutGenres());
  }

  private void applyTvDiscoverParams(UriBuilder builder) {
    builder
        .queryParam("include_adult", properties.isTvIncludeAdult())
        .queryParam("sort_by", properties.getTvSortBy());

    queryParamIfHasText(builder, "vote_count.gte", properties.getTvVoteCountGte());
    queryParamIfHasText(builder, "without_keywords", properties.getTvWithoutKeywords());
    queryParamIfHasText(builder, "without_genres", properties.getTvWithoutGenres());
  }

  private void queryParamIfHasText(UriBuilder builder, String name, String value) {
    if (StringUtils.hasText(value)) {
      builder.queryParam(name, value);
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

  private interface DiscoverParamCustomizer {

    void customize(UriBuilder builder);
  }
}
