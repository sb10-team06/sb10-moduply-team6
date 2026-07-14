package com.team6.moduply.testdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
@ConfigurationProperties(prefix = "moduply.test-data.conversation")
public class ConversationTestDataProperties {

  private boolean enabled = false;
  private int userSize = 10000;
  private String userEmailLike = "k6-conversation-%@moduply.test";
  private int conversationsPerUser = 100;
  private int messagesPerConversation = 10;
  private double unreadRatio = 0.3;
  private int chunkSize = 1000;
  private boolean skipIfExists = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getUserSize() {
    return userSize;
  }

  public void setUserSize(int userSize) {
    this.userSize = userSize;
  }

  public String getUserEmailLike() {
    return userEmailLike;
  }

  public void setUserEmailLike(String userEmailLike) {
    this.userEmailLike = userEmailLike;
  }

  public int getConversationsPerUser() {
    return conversationsPerUser;
  }

  public void setConversationsPerUser(int conversationsPerUser) {
    this.conversationsPerUser = conversationsPerUser;
  }

  public int getMessagesPerConversation() {
    return messagesPerConversation;
  }

  public void setMessagesPerConversation(int messagesPerConversation) {
    this.messagesPerConversation = messagesPerConversation;
  }

  public double getUnreadRatio() {
    return unreadRatio;
  }

  public void setUnreadRatio(double unreadRatio) {
    this.unreadRatio = unreadRatio;
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
