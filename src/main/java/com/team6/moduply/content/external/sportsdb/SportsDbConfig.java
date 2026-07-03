package com.team6.moduply.content.external.sportsdb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SportsDbProperties.class)
public class SportsDbConfig {

  @Bean
  @Qualifier("sportsDbRestClient")
  public RestClient sportsDbRestClient(
      RestClient.Builder builder,
      SportsDbProperties properties
  ) {
    return builder
        .baseUrl(properties.getBaseUrl())
        .requestFactory(requestFactory(properties))
        .build();
  }

  private SimpleClientHttpRequestFactory requestFactory(SportsDbProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
    requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
    return requestFactory;
  }
}
