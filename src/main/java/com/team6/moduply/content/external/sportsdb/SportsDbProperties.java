package com.team6.moduply.content.external.sportsdb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "moduply.external.sports-db")
public class SportsDbProperties {

  private String baseUrl = "https://www.thesportsdb.com/api/v1/json";

  private String apiKey = "123";
}
