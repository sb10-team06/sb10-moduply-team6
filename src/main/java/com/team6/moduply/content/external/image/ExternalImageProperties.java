package com.team6.moduply.content.external.image;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "moduply.external.image")
public class ExternalImageProperties {

  private int connectTimeoutMillis = 3000;

  private int readTimeoutMillis = 10000;
}
