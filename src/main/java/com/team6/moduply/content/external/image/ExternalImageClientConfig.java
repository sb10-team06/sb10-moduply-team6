package com.team6.moduply.content.external.image;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ExternalImageProperties.class)
public class ExternalImageClientConfig {

  @Bean
  public ExternalImageClient externalImageClient(
      RestClient.Builder restClientBuilder,
      ExternalImageProperties properties
  ) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
    requestFactory.setReadTimeout(properties.getReadTimeoutMillis());

    return new ExternalImageClient(restClientBuilder.requestFactory(requestFactory));
  }
}
