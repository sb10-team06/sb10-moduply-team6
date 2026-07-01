package com.team6.moduply.content.external.tmdb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

  @Bean
  @Qualifier("tmdbRestClient")
  public RestClient tmdbRestClient(
      RestClient.Builder builder,
      TmdbProperties properties
  ) {
    return builder
        .baseUrl(properties.getBaseUrl())
        .build();
  }
}
