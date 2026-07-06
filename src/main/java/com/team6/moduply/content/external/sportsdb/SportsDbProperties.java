package com.team6.moduply.content.external.sportsdb;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "moduply.external.sports-db")
public class SportsDbProperties {

  @NotBlank
  private String baseUrl = "https://www.thesportsdb.com/api/v1/json";

  @NotBlank
  private String apiKey;

  @Min(1)
  private int connectTimeoutMillis = 3000;

  @Min(1)
  private int readTimeoutMillis = 10000;
}
