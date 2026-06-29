package com.team6.moduply.binarycontent.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.s3.exception.S3ErrorCode;
import com.team6.moduply.binarycontent.s3.exception.S3StorageException;
import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3BinaryContentStorageTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private S3Presigner s3Presigner;

  private S3Properties properties;
  private S3BinaryContentStorage s3BinaryContentStorage;

  @BeforeEach
  void setUp() {
    properties = new S3Properties();
    properties.setBucket("test-bucket");
    properties.setPresignedUrlExpiration(600L);

    s3BinaryContentStorage = new S3BinaryContentStorage(s3Client, properties, s3Presigner);
  }

  @Test
  @DisplayName("S3 저장소는 BinaryContentStorage 구현체이다.")
  void storage_success_as_binary_content_storage() {
    // when & then
    assertThat(s3BinaryContentStorage).isInstanceOf(BinaryContentStorage.class);
  }

  @Test
  @DisplayName("S3 업로드에 성공하면 storage key를 반환한다.")
  void upload_success_with_storage_key() {
    // given
    String key = "users/user-id/profile/binary-content-id.png";
    byte[] bytes = "image-bytes".getBytes();
    String contentType = "image/png";

    // 실제 AWS로 요청하지 않도록 S3Client를 mock 처리한다.
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willReturn(PutObjectResponse.builder().build());

    // when
    String result = s3BinaryContentStorage.upload(key, bytes, contentType);

    // then
    assertThat(result).isEqualTo(key);

    // S3에 전달되는 bucket, key, contentType이 의도한 값인지 검증한다.
    ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

    PutObjectRequest request = requestCaptor.getValue();
    assertThat(request.bucket()).isEqualTo("test-bucket");
    assertThat(request.key()).isEqualTo(key);
    assertThat(request.contentType()).isEqualTo(contentType);
  }

  @Test
  @DisplayName("S3 업로드에 실패하면 S3UploadException을 던진다.")
  void upload_fail_when_s3_exception_occurred() {
    // given
    String key = "contents/content-id/images/binary-content-id.png";
    byte[] bytes = "image-bytes".getBytes();
    String contentType = "image/png";
    S3Exception cause = (S3Exception) S3Exception.builder()
        .message("S3 파일 업로드에 실패했습니다.")
        .build();

    // S3Client가 S3Exception을 던지는 상황을 만들어 실패 분기를 검증한다.
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willThrow(cause);

    // when & then
    assertThatThrownBy(() -> s3BinaryContentStorage.upload(key, bytes, contentType))
        .isInstanceOfSatisfying(S3StorageException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(S3ErrorCode.S3_UPLOAD_FAILED);
          assertThat(exception.getDetails().get("key")).isEqualTo(key);
          assertThat(exception.getCause()).isEqualTo(cause);
        });

    // 실패하더라도 S3 업로드 요청 자체는 한 번 시도되어야 한다.
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("S3 Presigned URL 생성에 성공하면 URL 문자열을 반환한다.")
  void generateUrl_success_with_presigned_url() throws Exception {
    // given
    String key = "contents/content-id/thumbnail/binary-content-id.png";
    String contentType = "image/png";
    String presignedUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/" + key;
    PresignedGetObjectRequest presignedRequest = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
    given(presignedRequest.url()).willReturn(new URI(presignedUrl).toURL());
    given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .willReturn(presignedRequest);

    // when
    String result = s3BinaryContentStorage.generateUrl(key, contentType);

    // then
    assertThat(result).isEqualTo(presignedUrl);

    ArgumentCaptor<GetObjectPresignRequest> requestCaptor =
        ArgumentCaptor.forClass(GetObjectPresignRequest.class);
    verify(s3Presigner).presignGetObject(requestCaptor.capture());

    GetObjectRequest getObjectRequest = requestCaptor.getValue().getObjectRequest();
    assertThat(getObjectRequest.bucket()).isEqualTo("test-bucket");
    assertThat(getObjectRequest.key()).isEqualTo(key);
    assertThat(getObjectRequest.responseContentType()).isEqualTo(contentType);
    assertThat(requestCaptor.getValue().signatureDuration().getSeconds()).isEqualTo(600L);
  }

  @Test
  @DisplayName("S3 삭제에 성공하면 storage key를 반환한다.")
  void delete_success_with_storage_key() {
    // given
    String key = "contents/content-id/thumbnail/binary-content-id.png";
    given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .willReturn(DeleteObjectResponse.builder().build());

    // when
    String result = s3BinaryContentStorage.delete(key);

    // then
    assertThat(result).isEqualTo(key);

    ArgumentCaptor<DeleteObjectRequest> requestCaptor =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(requestCaptor.capture());

    DeleteObjectRequest request = requestCaptor.getValue();
    assertThat(request.bucket()).isEqualTo("test-bucket");
    assertThat(request.key()).isEqualTo(key);
  }

  @Test
  @DisplayName("S3 삭제에 실패하면 S3StorageException을 던진다.")
  void delete_fail_when_s3_exception_occurred() {
    // given
    String key = "contents/content-id/thumbnail/binary-content-id.png";
    S3Exception cause = (S3Exception) S3Exception.builder()
        .message("S3 파일 삭제에 실패했습니다.")
        .build();
    given(s3Client.deleteObject(any(DeleteObjectRequest.class))).willThrow(cause);

    // when & then
    assertThatThrownBy(() -> s3BinaryContentStorage.delete(key))
        .isInstanceOfSatisfying(S3StorageException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(S3ErrorCode.S3_DELETE_FAILED);
          assertThat(exception.getDetails().get("key")).isEqualTo(key);
          assertThat(exception.getCause()).isEqualTo(cause);
        });

    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
  }

}
