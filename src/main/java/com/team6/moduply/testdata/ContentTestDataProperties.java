package com.team6.moduply.testdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
@ConfigurationProperties(prefix = "moduply.test-data.content")
public class ContentTestDataProperties {

  private boolean enabled = false;
  private int totalSize = 10000;
  private int chunkSize = 1000;
  private boolean skipIfExists = true;
  private String imageBaseUrl = "https://moduply-test.s3.ap-northeast-2.amazonaws.com";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(int totalSize) {
    this.totalSize = totalSize;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public boolean isSkipIfExists() {
    return skipIfExists;
  }

  public void setSkipIfExists(boolean skipIfExists) {
    this.skipIfExists = skipIfExists;
  }

  public String getImageBaseUrl() {
    return imageBaseUrl;
  }

  public void setImageBaseUrl(String imageBaseUrl) {
    this.imageBaseUrl = imageBaseUrl;
  }
}
