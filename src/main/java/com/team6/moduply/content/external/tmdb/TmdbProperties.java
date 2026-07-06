package com.team6.moduply.content.external.tmdb;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "moduply.external.tmdb")
public class TmdbProperties {

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
