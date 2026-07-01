package com.team6.moduply.content.external.tmdb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "moduply.external.tmdb")
public class TmdbProperties {

  private String baseUrl = "https://api.themoviedb.org/3";

  private String imageBaseUrl = "https://image.tmdb.org/t/p";

  private String readAccessToken;
}
