package com.team6.moduply.binarycontent.s3;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
@ConditionalOnProperty(name = "moduply.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3Config {

  private final S3Properties properties;

  public S3Config(S3Properties properties) {
    this.properties = properties;
  }

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .region(region())
        .credentialsProvider(credentialsProvider())
        .httpClientBuilder(s3HttpClientBuilder())
        .overrideConfiguration(s3ClientOverrideConfiguration())
        .build();
  }

  private Region region() {
    String region = properties.getRegion();
    if (region == null || region.isBlank()) {
      throw new IllegalStateException("AWS_S3_REGION 설정이 누락되었습니다.");
    }

    return Region.of(region);
  }

  private AwsCredentialsProvider credentialsProvider() {
    boolean hasAccessKey =
        properties.getAccessKey() != null && !properties.getAccessKey().isBlank();
    boolean hasSecretKey =
        properties.getSecretKey() != null && !properties.getSecretKey().isBlank();

    if (hasAccessKey ^ hasSecretKey) {
      throw new IllegalArgumentException(
          "S3 access key와 secret key는 함께 설정되어야 합니다."
      );
    }

    return hasAccessKey
        ? StaticCredentialsProvider.create(
            AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
        )
        : DefaultCredentialsProvider.create();
  }

  private ClientOverrideConfiguration s3ClientOverrideConfiguration() {
    return ClientOverrideConfiguration.builder()
        .retryPolicy(RetryPolicy.builder()
            .numRetries(properties.getMaxRetries())
            .build())
        .apiCallTimeout(Duration.ofMillis(properties.getApiCallTimeoutMillis()))
        .apiCallAttemptTimeout(Duration.ofMillis(properties.getApiCallAttemptTimeoutMillis()))
        .build();
  }

  private ApacheHttpClient.Builder s3HttpClientBuilder() {
    return ApacheHttpClient.builder()
        .connectionTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
        .socketTimeout(Duration.ofMillis(properties.getSocketTimeoutMillis()))
        .connectionAcquisitionTimeout(
            Duration.ofMillis(properties.getConnectionAcquisitionTimeoutMillis())
        );
  }
}
