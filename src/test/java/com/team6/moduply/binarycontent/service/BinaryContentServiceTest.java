package com.team6.moduply.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.BinaryContentStatus;
import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.s3.S3BinaryContentStorage;
import com.team6.moduply.binarycontent.s3.exception.S3ErrorCode;
import com.team6.moduply.binarycontent.s3.exception.S3UploadException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BinaryContentServiceTest {

  @Mock
  private S3BinaryContentStorage s3BinaryContentStorage;

  @Mock
  private BinaryContentRepository binaryContentRepository;

  @InjectMocks
  private BinaryContentService binaryContentService;

  @Test
  @DisplayName("프로필 이미지 생성에 성공하면 S3 업로드 후 BinaryContent를 SUCCESS 상태로 저장한다.")
  void createUserProfile_success_with_user() throws IOException {
    // given
    // 사용자id 생성
    UUID userId = UUID.randomUUID();
    // MultipartFile 생성
    MockMultipartFile image = createImage("profile.png", "image/png");
    // S3 upload 동작: key 반환
    given(s3BinaryContentStorage.upload(any(String.class), any(byte[].class), eq("image/png")))
        .willAnswer(invocation -> invocation.getArgument(0));
    // BinaryContent save: BinaryContent 반환
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    // PresignedUrl 생성
    given(s3BinaryContentStorage.generatePresignedUrl(any(String.class), eq("image/png")))
        .willReturn("https://example.com/profile.png");

    // when
    BinaryContent result = binaryContentService.createUserProfile(userId, image);

    // then
    assertThat(result.getFileName()).isEqualTo("profile.png");
    assertThat(result.getSize()).isEqualTo(image.getSize());
    assertThat(result.getContentType()).isEqualTo("image/png");
    assertThat(result.getStorageKey())
        .startsWith("users/%s/profile/".formatted(userId))
        .endsWith(".png");
    assertThat(result.getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);

    verify(s3BinaryContentStorage).upload(
        eq(result.getStorageKey()),
        eq(image.getBytes()),
        eq("image/png")
    );
    verify(binaryContentRepository).save(result);
    verify(s3BinaryContentStorage).generatePresignedUrl(result.getStorageKey(), "image/png");
  }

  @Test
  @DisplayName("프로필 이미지 S3 업로드에 실패하면 BinaryContent를 FAIL 상태로 저장하고 S3UploadException을 던진다.")
  void createUserProfile_fail_when_s3_upload_failed() {
    // given
    UUID userId = UUID.randomUUID();
    MockMultipartFile image = createImage("profile.png", "image/png");
    RuntimeException cause = new RuntimeException("upload failed");
    given(s3BinaryContentStorage.upload(any(String.class), any(byte[].class), eq("image/png")))
        .willThrow(cause);
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when & then
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, image))
        .isInstanceOfSatisfying(S3UploadException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(S3ErrorCode.S3_UPLOAD_FAILED);
          assertThat(exception.getDetails().get("userId")).isEqualTo(userId);
          assertThat(exception.getDetails().get("storageKey")).isInstanceOf(String.class);
          assertThat(exception.getCause()).isEqualTo(cause);
        });

    ArgumentCaptor<BinaryContent> captor = ArgumentCaptor.forClass(BinaryContent.class);
    verify(binaryContentRepository).save(captor.capture());
    BinaryContent failed = captor.getValue();
    assertThat(failed.getStatus()).isEqualTo(BinaryContentStatus.FAIL);
    assertThat(failed.getStorageKey())
        .startsWith("users/%s/profile/".formatted(userId))
        .endsWith(".png");
  }

  @Test
  @DisplayName("콘텐츠 이미지 생성에 성공하면 S3 업로드 후 BinaryContent를 SUCCESS 상태로 저장한다.")
  void createContentImage_success_with_content() throws IOException {
    // given
    UUID contentId = UUID.randomUUID();
    MockMultipartFile image = createImage("thumbnail.webp", "image/webp");
    given(s3BinaryContentStorage.upload(any(String.class), any(byte[].class), eq("image/webp")))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(s3BinaryContentStorage.generatePresignedUrl(any(String.class), eq("image/webp")))
        .willReturn("https://example.com/thumbnail.webp");

    // when
    BinaryContent result = binaryContentService.createContentImage(contentId, image);

    // then
    assertThat(result.getFileName()).isEqualTo("thumbnail.webp");
    assertThat(result.getSize()).isEqualTo(image.getSize());
    assertThat(result.getContentType()).isEqualTo("image/webp");
    assertThat(result.getStorageKey())
        .startsWith("contents/%s/thumbnail/".formatted(contentId))
        .endsWith(".webp");
    assertThat(result.getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);

    verify(s3BinaryContentStorage).upload(
        eq(result.getStorageKey()),
        eq(image.getBytes()),
        eq("image/webp")
    );
    verify(binaryContentRepository).save(result);
    verify(s3BinaryContentStorage).generatePresignedUrl(result.getStorageKey(), "image/webp");
  }

  @Test
  @DisplayName("콘텐츠 이미지 S3 업로드에 실패하면 BinaryContent를 FAIL 상태로 저장하고 S3UploadException을 던진다.")
  void createContentImage_fail_when_s3_upload_failed() {
    // given
    UUID contentId = UUID.randomUUID();
    MockMultipartFile image = createImage("thumbnail.jpeg", "image/jpeg");
    RuntimeException cause = new RuntimeException("upload failed");
    given(s3BinaryContentStorage.upload(any(String.class), any(byte[].class), eq("image/jpeg")))
        .willThrow(cause);
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, image))
        .isInstanceOfSatisfying(S3UploadException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(S3ErrorCode.S3_UPLOAD_FAILED);
          assertThat(exception.getDetails().get("contentId")).isEqualTo(contentId);
          assertThat(exception.getDetails().get("storageKey")).isInstanceOf(String.class);
          assertThat(exception.getCause()).isEqualTo(cause);
        });

    ArgumentCaptor<BinaryContent> captor = ArgumentCaptor.forClass(BinaryContent.class);
    verify(binaryContentRepository).save(captor.capture());
    BinaryContent failed = captor.getValue();
    assertThat(failed.getStatus()).isEqualTo(BinaryContentStatus.FAIL);
    assertThat(failed.getStorageKey())
        .startsWith("contents/%s/thumbnail/".formatted(contentId))
        .endsWith(".jpeg");
  }

  private MockMultipartFile createImage(String fileName, String contentType) {
    return new MockMultipartFile(
        "image",
        fileName,
        contentType,
        "image-bytes".getBytes(StandardCharsets.UTF_8)
    );
  }
}
