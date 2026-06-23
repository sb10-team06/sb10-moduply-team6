package com.team6.moduply.binarycontent.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/// Spring 생성시 S3Client Bean을 생성하는 설정 클래스
/// s3Client 객체 생성해서 region과 credentials(accessKey + secretKey)를 담고 S3업로드 부분에서 사용한다.
/// S3Client: AWS와 통신하는 객체.
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    /// application.yml에서 주입받은 AWS 값들
    private final S3Properties properties;

    public S3Config(S3Properties properties) {this.properties = properties;}

    @Bean
    public S3Client s3Client() {
        String region = properties.getRegion();
        if (region == null || region.isBlank()) {
            throw new IllegalStateException("AWS_S3_REGION 설정이 누락되었습니다.");
        }

        /// accessKey와 secretKey 둘중 하나만 있으면 예외처리: 둘다있어야 AWS 인증이 되기때문.
        // accessKey 존재여부 확인
        boolean hasAccessKey = properties.getAccessKey() != null && !properties.getAccessKey().isBlank();
        // secretKey 존재여부 확인
        boolean hasSecretKey = properties.getSecretKey() != null && !properties.getSecretKey().isBlank();
        if (hasAccessKey || hasSecretKey) {
            // 둘중 하나만 있는경우
            if(!(hasAccessKey && hasSecretKey)) {
                throw new IllegalArgumentException("S3 access key와 secret key는 함께 설정되어야 합니다.");
            }
            // 둘다 정상 존재할 경우
            return S3Client.builder()
                    // region 설정
                    .region(Region.of(properties.getRegion()))
                    // 인증정보 설정
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(
                                            properties.getAccessKey(),
                                            properties.getSecretKey()
                                    )
                            )
                    )
                    .build();
        }

        // 그렇지 않으면: 기본 체인(환경변수, 프로파일, IAM Role)을 자동 탐색
        // 키가 없는경우
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

    }
}
