package com.team6.moduply.binarycontent.s3;

import com.team6.moduply.binarycontent.s3.exception.S3ErrorCode;
import com.team6.moduply.binarycontent.s3.exception.S3UploadException;
import com.team6.moduply.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3BinaryContentStorage {
    private final S3Client s3Client;
    private final S3Properties properties;
    private final S3Presigner s3Presigner;



    //TODO 재시도 전략 설정 추후 필요.
    /// user프로필, content이미지 파일 upload
    /// key: 이미지가 S3에 저장되는 경로.
    /// user의 프로필: String key = "users/%s/profile/%s".formatted(userId, binaryContentId);
    /// content의 이미지: String key = "contents/%s/images/%s".formatted(contentId, binaryContentId);
    public String upload(String key, byte[] bytes, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));

            return key;
        } catch (S3Exception e) {
            /// S3Exception 원본 원인 추적이 가능하도록 cause 전달 처리
            throw new S3UploadException(S3ErrorCode.S3_UPLOAD_FAILED, Map.of("key", key), e);
        }
    }

    //TODO upload() 재시도전략도 실패시 복구메서드 추후 필요.

    /// 비공개 bucket의 경우 presigner 필요.
    /// Presigner를 이용해서 특정 S3 객체에 접근 가능한 임시 URL 생성
    public String generatePresignedUrl(String key, String contentType) {
        // S3Presigner 생성

            // S3객체를 다운로드/조회하는 요청만들기
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .responseContentType(contentType)
                    .build();

            //Presigned URL 설정, 유효시간 10분으로 설정함.
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(properties.getPresignedUrlExpiration()))
                    .getObjectRequest(getObjectRequest)
                    .build();

            // URL 생성
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();

    }


}
