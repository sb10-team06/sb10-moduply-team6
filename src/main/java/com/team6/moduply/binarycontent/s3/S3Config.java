package com.team6.moduply.binarycontent.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/// Spring 생성시 S3Client Bean을 생성하는 설정 클래스
/// s3Client 객체 생성해서 region과 credentials(accessKey + secretKey)를 담고 S3업로드 부분에서 사용한다.
/// S3Client: AWS와 통신하는 객체.
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@ConditionalOnProperty(name = "moduply.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3Config {

    /// application.yml에서 주입받은 AWS 값들
    private final S3Properties properties;

    public S3Config(S3Properties properties) {this.properties = properties;}

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(region())
                .credentialsProvider(credentialsProvider())
                .build();

    }

    @Bean
    // AWS 인증 정보를 가진 Presigner 객체를 생성
    public S3Presigner getS3Presigner() {
        return S3Presigner.builder()
                // 리전설정
                .region(region())
                .credentialsProvider(credentialsProvider())
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
        /// accessKey와 secretKey 둘중 하나만 있으면 예외처리: 둘다있어야 AWS 인증이 되기때문.
        // accessKey 존재여부 확인
        boolean hasAccessKey = properties.getAccessKey() != null && !properties.getAccessKey().isBlank();
        // secretKey 존재여부 확인
        boolean hasSecretKey = properties.getSecretKey() != null && !properties.getSecretKey().isBlank();

        // 둘중 하나만 있는경우: xor로 판단
        if (hasAccessKey ^ hasSecretKey) {
            throw new IllegalArgumentException("S3 access key와 secret key는 함께 설정되어야 합니다.");
        }

        /// hasAccessKey ^ hasSecretKey로 둘다있어나, 둘다 없거나 조건만 탄다.
        // 둘다 있으면: accesskey와 secretkey를 이용해서 AWS 요청에 사용할 인증 정보 객체 만든다.
        // 둘다 없으면: AWS SDK가 정해진 순서대로 인증정보를 찾는다.
        return hasAccessKey
                ? StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            properties.getAccessKey(),
                            properties.getSecretKey()
                    )
                )
                : DefaultCredentialsProvider.create();
    }
}
