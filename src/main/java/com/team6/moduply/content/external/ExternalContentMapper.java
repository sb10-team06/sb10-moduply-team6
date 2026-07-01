package com.team6.moduply.content.external;

import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.external.sportsdb.dto.SportsDbEventResponse;
import com.team6.moduply.content.external.tmdb.TmdbProperties;
import com.team6.moduply.content.external.tmdb.dto.TmdbMovieResponse;
import com.team6.moduply.content.external.tmdb.dto.TmdbTvResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ExternalContentMapper {

  private static final String TMDB_DEFAULT_IMAGE_SIZE = "w500";

  private final TmdbProperties tmdbProperties;

  public ExternalContentItem toItem(TmdbMovieResponse response) {
    validateTmdbMovie(response);

    return new ExternalContentItem(
        "tmdb:movie:%d".formatted(response.id()),
        ContentType.movie,
        response.title(),
        defaultIfBlank(response.overview(), response.title()),
        buildTmdbImageUrl(response.posterPath(), response.backdropPath()),
        List.of("TMDB", "movie")
    );
  }

  public ExternalContentItem toItem(TmdbTvResponse response) {
    validateTmdbTv(response);

    return new ExternalContentItem(
        "tmdb:tv:%d".formatted(response.id()),
        ContentType.tvSeries,
        response.name(),
        defaultIfBlank(response.overview(), response.name()),
        buildTmdbImageUrl(response.posterPath(), response.backdropPath()),
        List.of("TMDB", "tvSeries")
    );
  }

  public ExternalContentItem toItem(SportsDbEventResponse response) {
    validateSportsDbEvent(response);

    return new ExternalContentItem(
        "sportsdb:event:%s".formatted(response.idEvent()),
        ContentType.sport,
        response.eventName(),
        defaultIfBlank(response.description(), sportsDescription(response)),
        blankToNull(response.thumbnailUrl()),
        List.of("SportsDB", response.sport(), response.league()).stream()
            .filter(StringUtils::hasText)
            .toList()
    );
  }

  private void validateTmdbMovie(TmdbMovieResponse response) {
    if (response == null
        || response.id() == null
        || !StringUtils.hasText(response.title())) {
      throw invalidExternalContent("TMDB", "movie", "id/title");
    }
  }

  private void validateTmdbTv(TmdbTvResponse response) {
    if (response == null
        || response.id() == null
        || !StringUtils.hasText(response.name())) {
      throw invalidExternalContent("TMDB", "tv", "id/name");
    }
  }

  private void validateSportsDbEvent(SportsDbEventResponse response) {
    if (response == null
        || !StringUtils.hasText(response.idEvent())
        || !StringUtils.hasText(response.eventName())) {
      throw invalidExternalContent("SportsDB", "event", "idEvent/strEvent");
    }
  }

  private ContentException invalidExternalContent(
      String source,
      String contentType,
      String requiredFields
  ) {
    return new ContentException(
        ContentErrorCode.EXTERNAL_CONTENT_INVALID_RESPONSE,
        Map.of(
            "source", source,
            "contentType", contentType,
            "requiredFields", requiredFields
        )
    );
  }

  private String sportsDescription(SportsDbEventResponse response) {
    return "%s %s %s".formatted(
        defaultIfBlank(response.league(), "Sports"),
        defaultIfBlank(response.eventDate(), ""),
        response.eventName()
    ).trim();
  }

  private String defaultIfBlank(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value : defaultValue;
  }

  private String buildTmdbImageUrl(String posterPath, String backdropPath) {
    String imagePath = StringUtils.hasText(posterPath) ? posterPath : backdropPath;

    if (!StringUtils.hasText(imagePath)) {
      return null;
    }

    String normalizedPath = imagePath.startsWith("/") ? imagePath : "/" + imagePath;

    return "%s/%s%s".formatted(
        tmdbProperties.getImageBaseUrl(),
        TMDB_DEFAULT_IMAGE_SIZE,
        normalizedPath
    );
  }

  private String blankToNull(String value) {
    return StringUtils.hasText(value) ? value : null;
  }
}
