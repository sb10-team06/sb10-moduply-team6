package com.team6.moduply.binarycontent.storage.local;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(LocalStorageProperties.class)
@ConditionalOnProperty(name = "moduply.storage.type", havingValue = "local")
public class LocalFileResourceConfig implements WebMvcConfigurer {

  private final LocalStorageProperties properties;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler(normalizeUrlPrefix() + "/**")
        .addResourceLocations("file:" + normalizeRootPath() + "/");
  }

  private String normalizeUrlPrefix() {
    String urlPrefix = properties.getUrlPrefix();
    if (urlPrefix.endsWith("/")) {
      return urlPrefix.substring(0, urlPrefix.length() - 1);
    }
    return urlPrefix;
  }

  private String normalizeRootPath() {
    String rootPath = properties.getRootPath();
    if (rootPath.endsWith("/")) {
      return rootPath.substring(0, rootPath.length() - 1);
    }
    return rootPath;
  }
}
