package com.team6.moduply.binarycontent.s3;

import com.team6.moduply.binarycontent.s3.exception.S3ErrorCode;
import com.team6.moduply.binarycontent.s3.exception.S3StorageException;
import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "moduply.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3BinaryContentStorage implements BinaryContentStorage {

  private final S3Client s3Client;
  private final S3Properties properties;

  @Override
  public String upload(String key, byte[] bytes, String contentType) {
    try {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(properties.getBucket())
          .key(key)
          .contentType(contentType)
          .build();

      s3Client.putObject(request, RequestBody.fromBytes(bytes));

      GetUrlRequest getUrlRequest = GetUrlRequest.builder()
          .bucket(properties.getBucket())
          .key(key)
          .build();
      return s3Client.utilities().getUrl(getUrlRequest).toExternalForm();
    } catch (S3Exception e) {
      throw new S3StorageException(S3ErrorCode.S3_UPLOAD_FAILED, Map.of("key", key), e);
    }
  }

  @Override
  public String delete(String key) {
    try {
      DeleteObjectRequest request = DeleteObjectRequest.builder()
          .bucket(properties.getBucket())
          .key(key)
          .build();

      s3Client.deleteObject(request);
      log.info("S3 파일 삭제 완료. key={}", key);
      return key;
    } catch (S3Exception e) {
      throw new S3StorageException(S3ErrorCode.S3_DELETE_FAILED, Map.of("key", key), e);
    }
  }
}
