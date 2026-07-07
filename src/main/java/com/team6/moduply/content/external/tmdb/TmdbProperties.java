package com.team6.moduply.content.external.tmdb;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "moduply.external.tmdb")
public class TmdbProperties {

  private boolean movieIncludeAdult = false;

  private boolean movieIncludeVideo = false;

  @NotBlank
  private String movieSortBy = "primary_release_date.desc";

  private String movieRegion = "KR";

  private String movieCertificationCountry = "KR";

  private String movieCertificationLte = "15";

  @Pattern(regexp = "\\d*", message = "movieVoteCountGte는 숫자만 입력할 수 있습니다.")
  private String movieVoteCountGte = "10";

  private String movieWithoutKeywords;

  private String movieWithoutGenres;

  private boolean tvIncludeAdult = false;

  @NotBlank
  private String tvSortBy = "first_air_date.desc";

  @Pattern(regexp = "\\d*", message = "tvVoteCountGte는 숫자만 입력할 수 있습니다.")
  private String tvVoteCountGte = "10";

  private String tvWithoutKeywords;

  private String tvWithoutGenres;

  @NotBlank
  private String baseUrl = "https://api.themoviedb.org/3";

  @NotBlank
  private String imageBaseUrl = "https://image.tmdb.org/t/p";

  @NotBlank
  private String readAccessToken;

  @Min(1)
  private int connectTimeoutMillis = 3000;

  @Min(1)
  private int readTimeoutMillis = 10000;
}
