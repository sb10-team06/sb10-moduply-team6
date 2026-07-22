package com.team6.moduply.binarycontent.s3;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

///S3설정값을 Java 객체로 바인딩하는 설정 클래스
@Getter
@Setter
@ConfigurationProperties(prefix = "moduply.storage.s3")
public class S3Properties {
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private Long presignedUrlExpiration;
    private Long connectTimeoutMillis = 3000L;
    private Long socketTimeoutMillis = 5000L;
    private Long connectionAcquisitionTimeoutMillis = 3000L;
    private Long apiCallTimeoutMillis = 10000L;
    private Long apiCallAttemptTimeoutMillis = 5000L;
    private Integer maxRetries = 1;
}
