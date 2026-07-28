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
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3BinaryContentStorageTest {

  @Mock
  private S3Client s3Client;
  @Mock
  private S3Utilities s3Utilities;

  private S3BinaryContentStorage storage;

  @BeforeEach
  void setUp() {
    S3Properties properties = new S3Properties();
    properties.setBucket("test-bucket");
    storage = new S3BinaryContentStorage(s3Client, properties);
  }

  @Test
  @DisplayName("S3 저장소는 BinaryContentStorage 구현체다.")
  void storage_success_as_binary_content_storage() {
    assertThat(storage).isInstanceOf(BinaryContentStorage.class);
  }

  @Test
  @DisplayName("S3 업로드에 성공하면 객체 URL을 반환한다.")
  void upload_success_with_object_url() throws Exception {
    String key = "users/user-id/profile/binary-content-id.png";
    byte[] bytes = "image-bytes".getBytes();
    String objectUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/" + key;
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willReturn(PutObjectResponse.builder().build());
    given(s3Client.utilities()).willReturn(s3Utilities);
    given(s3Utilities.getUrl(any(GetUrlRequest.class)))
        .willReturn(new URI(objectUrl).toURL());

    String result = storage.upload(key, bytes, "image/png");

    assertThat(result).isEqualTo(objectUrl);
    ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
    assertThat(putCaptor.getValue().bucket()).isEqualTo("test-bucket");
    assertThat(putCaptor.getValue().key()).isEqualTo(key);
    assertThat(putCaptor.getValue().contentType()).isEqualTo("image/png");

    ArgumentCaptor<GetUrlRequest> urlCaptor = ArgumentCaptor.forClass(GetUrlRequest.class);
    verify(s3Utilities).getUrl(urlCaptor.capture());
    assertThat(urlCaptor.getValue().bucket()).isEqualTo("test-bucket");
    assertThat(urlCaptor.getValue().key()).isEqualTo(key);
  }

  @Test
  @DisplayName("S3 업로드에 실패하면 S3StorageException을 던진다.")
  void upload_fail_when_s3_exception_occurred() {
    String key = "contents/content-id/images/binary-content-id.png";
    S3Exception cause = (S3Exception) S3Exception.builder().message("upload failed").build();
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willThrow(cause);

    assertThatThrownBy(() -> storage.upload(key, "image-bytes".getBytes(), "image/png"))
        .isInstanceOfSatisfying(S3StorageException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(S3ErrorCode.S3_UPLOAD_FAILED);
          assertThat(exception.getDetails().get("key")).isEqualTo(key);
          assertThat(exception.getCause()).isEqualTo(cause);
        });
  }

  @Test
  @DisplayName("S3 삭제에 성공하면 storageKey를 반환한다.")
  void delete_success_with_storage_key() {
    String key = "contents/content-id/thumbnail/binary-content-id.png";
    given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .willReturn(DeleteObjectResponse.builder().build());

    String result = storage.delete(key);

    assertThat(result).isEqualTo(key);
    ArgumentCaptor<DeleteObjectRequest> requestCaptor =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(requestCaptor.capture());
    assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
    assertThat(requestCaptor.getValue().key()).isEqualTo(key);
  }

  @Test
  @DisplayName("S3 삭제에 실패하면 S3StorageException을 던진다.")
  void delete_fail_when_s3_exception_occurred() {
    String key = "contents/content-id/thumbnail/binary-content-id.png";
    S3Exception cause = (S3Exception) S3Exception.builder().message("delete failed").build();
    given(s3Client.deleteObject(any(DeleteObjectRequest.class))).willThrow(cause);

    assertThatThrownBy(() -> storage.delete(key))
        .isInstanceOfSatisfying(S3StorageException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(S3ErrorCode.S3_DELETE_FAILED);
          assertThat(exception.getDetails().get("key")).isEqualTo(key);
          assertThat(exception.getCause()).isEqualTo(cause);
        });
  }
}
