package com.team6.moduply.binarycontent.storage.local;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "moduply.storage.local")
public class LocalStorageProperties {

  private String rootPath;
  private String urlPrefix;
}
