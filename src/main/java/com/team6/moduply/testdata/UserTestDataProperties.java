package com.team6.moduply.testdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
@ConfigurationProperties(prefix = "moduply.test-data.user")
public class UserTestDataProperties {

  private boolean enabled = false;
  private int followerSize = 100;
  private int followeeSize = 1000;
  private int conversationSize = 0;
  private int profileUpdateSize = 0;
  private int chunkSize = 500;
  private boolean skipIfExists = true;
  private String password = "k6-password";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getFollowerSize() {
    return followerSize;
  }

  public void setFollowerSize(int followerSize) {
    this.followerSize = followerSize;
  }

  public int getFolloweeSize() {
    return followeeSize;
  }

  public void setFolloweeSize(int followeeSize) {
    this.followeeSize = followeeSize;
  }

  public int getConversationSize() {
    return conversationSize;
  }

  public void setConversationSize(int conversationSize) {
    this.conversationSize = conversationSize;
  }

  public int getProfileUpdateSize() {
    return profileUpdateSize;
  }

  public void setProfileUpdateSize(int profileUpdateSize) {
    this.profileUpdateSize = profileUpdateSize;
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
