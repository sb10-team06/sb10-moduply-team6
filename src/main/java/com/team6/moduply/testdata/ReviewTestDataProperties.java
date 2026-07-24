package com.team6.moduply.testdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
@ConfigurationProperties(prefix = "moduply.test-data.review")
public class ReviewTestDataProperties {

  private boolean enabled = false;
  private int hotContentSize = 10;
  private int reviewsPerContent = 100000;
  private String userEmailLike = "k6-review-%@moduply.test";
  private String contentExternalApiIdLike = "k6-seed-%";
  private int chunkSize = 1000;
  private boolean skipIfExists = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getHotContentSize() {
    return hotContentSize;
  }

  public void setHotContentSize(int hotContentSize) {
    this.hotContentSize = hotContentSize;
  }

  public int getReviewsPerContent() {
    return reviewsPerContent;
  }

  public void setReviewsPerContent(int reviewsPerContent) {
    this.reviewsPerContent = reviewsPerContent;
  }

  public String getUserEmailLike() {
    return userEmailLike;
  }

  public void setUserEmailLike(String userEmailLike) {
    this.userEmailLike = userEmailLike;
  }

  public String getContentExternalApiIdLike() {
    return contentExternalApiIdLike;
  }

  public void setContentExternalApiIdLike(String contentExternalApiIdLike) {
    this.contentExternalApiIdLike = contentExternalApiIdLike;
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
}
