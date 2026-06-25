package com.team6.moduply.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.BinaryContentStatus;
import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.event.BinaryContentCreatedEvent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.s3.S3BinaryContentStorage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class BinaryContentServiceTest {

  @Mock
  private S3BinaryContentStorage s3BinaryContentStorage;

  @Mock
  private BinaryContentRepository binaryContentRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private BinaryContentService binaryContentService;

  @Test
  @DisplayName("프로필 이미지 생성 시 BinaryContent를 PROCESSING 상태로 저장하고 S3 업로드 이벤트를 발행한다.")
  void createUserProfile_success_publish_event() throws IOException {
    // given
    // userId 생성
    UUID userId = UUID.randomUUID();
    // 이미지 생성
    MockMultipartFile image = createImage("profile.png", "image/png");
    // binaryContent 저장
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> saveWithId(invocation.getArgument(0)));

    // when
    BinaryContent result = binaryContentService.createUserProfile(userId, image);

    // then
    // binaryContent가 잘 저장됐는지
    assertThat(result.getFileName()).isEqualTo("profile.png");
    assertThat(result.getSize()).isEqualTo(image.getSize());
    assertThat(result.getContentType()).isEqualTo("image/png");
    assertThat(result.getStorageKey())
        .startsWith("users/%s/profile/".formatted(userId))
        .endsWith(".png");
    assertThat(result.getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);
    //binaryContent.save() 실행됐는지
    verify(binaryContentRepository).save(result);

    //event 발행 잘 됐는지
    ArgumentCaptor<BinaryContentCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(BinaryContentCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    BinaryContentCreatedEvent event = eventCaptor.getValue();
    assertThat(event.getBinaryContentId()).isEqualTo(result.getId());
    assertThat(event.getBytes()).isEqualTo(image.getBytes());
    assertThat(event.getUserId()).isEqualTo(userId);
    assertThat(event.getContentId()).isNull();
  }

  @Test
  @DisplayName("프로필 이미지 생성 시 지원하지 않는 이미지 타입이면 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createUserProfile_fail_when_unsupported_image_type() {
    // given
    UUID userId = UUID.randomUUID();
    /// 지원하지않는 .gif타입인 이미지 생성
    MockMultipartFile image = createImage("profile.gif", "image/gif");

    // when & then
    /// UNSUPPORTED_IMAGE_TYPE 예외 발생됐는지?
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, image))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.UNSUPPORTED_IMAGE_TYPE)
        );

    /// save랑 evnet 발행 안됐는지?
    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("프로필 이미지 생성 시 파일이 없으면 EMPTY_FILE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createUserProfile_fail_when_image_is_null() {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, null))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.EMPTY_FILE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("프로필 이미지 생성 시 빈 파일이면 EMPTY_FILE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createUserProfile_fail_when_image_is_empty() {
    // given
    UUID userId = UUID.randomUUID();
    MockMultipartFile image = createEmptyImage("profile.png", "image/png");

    // when & then
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, image))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.EMPTY_FILE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("프로필 이미지 생성 시 파일 크기가 0 이하이면 INVALID_FILE_SIZE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createUserProfile_fail_when_image_size_is_invalid() {
    // given
    UUID userId = UUID.randomUUID();
    MultipartFile image = createZeroSizeImage("profile.png", "image/png");

    // when & then
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, image))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.INVALID_FILE_SIZE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("콘텐츠 이미지 생성 시 BinaryContent를 PROCESSING 상태로 저장하고 S3 업로드 이벤트를 발행한다.")
  void createContentImage_success_publish_event() throws IOException {
    // given
    UUID contentId = UUID.randomUUID();
    MockMultipartFile image = createImage("thumbnail.webp", "image/webp");
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> saveWithId(invocation.getArgument(0)));

    // when
    BinaryContent result = binaryContentService.createContentImage(contentId, image);

    // then
    assertThat(result.getFileName()).isEqualTo("thumbnail.webp");
    assertThat(result.getSize()).isEqualTo(image.getSize());
    assertThat(result.getContentType()).isEqualTo("image/webp");
    assertThat(result.getStorageKey())
        .startsWith("contents/%s/thumbnail/".formatted(contentId))
        .endsWith(".webp");
    assertThat(result.getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);

    verify(binaryContentRepository).save(result);

    ArgumentCaptor<BinaryContentCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(BinaryContentCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    BinaryContentCreatedEvent event = eventCaptor.getValue();
    assertThat(event.getBinaryContentId()).isEqualTo(result.getId());
    assertThat(event.getBytes()).isEqualTo(image.getBytes());
    assertThat(event.getUserId()).isNull();
    assertThat(event.getContentId()).isEqualTo(contentId);
  }

  @Test
  @DisplayName("콘텐츠 이미지 생성 시 지원하지 않는 이미지 타입이면 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createContentImage_fail_when_unsupported_image_type() {
    // given
    UUID contentId = UUID.randomUUID();
    MockMultipartFile image = createImage("thumbnail.gif", "image/gif");

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, image))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.UNSUPPORTED_IMAGE_TYPE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("콘텐츠 이미지 생성 시 파일이 없으면 EMPTY_FILE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createContentImage_fail_when_image_is_null() {
    // given
    UUID contentId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, null))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.EMPTY_FILE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("콘텐츠 이미지 생성 시 빈 파일이면 EMPTY_FILE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createContentImage_fail_when_image_is_empty() {
    // given
    UUID contentId = UUID.randomUUID();
    MockMultipartFile image = createEmptyImage("thumbnail.png", "image/png");

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, image))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.EMPTY_FILE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("콘텐츠 이미지 생성 시 파일 크기가 0 이하이면 INVALID_FILE_SIZE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createContentImage_fail_when_image_size_is_invalid() {
    // given
    UUID contentId = UUID.randomUUID();
    MultipartFile image = createZeroSizeImage("thumbnail.png", "image/png");

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, image))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.INVALID_FILE_SIZE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("S3 업로드 성공 처리 시 BinaryContent 상태를 SUCCESS로 변경한다.")
  void updatesStatusSuccess_success() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    BinaryContent binaryContent = BinaryContent.create(
        "profile.png",
        100L,
        "image/png",
        "users/%s/profile/test.png".formatted(UUID.randomUUID())
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));

    // when
    binaryContentService.updatesStatusSuccess(binaryContentId);

    // then
    assertThat(binaryContent.getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);
  }

  @Test
  @DisplayName("S3 업로드 실패 처리 시 BinaryContent 상태를 FAIL로 변경한다.")
  void updatesStatusFail_success() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    BinaryContent binaryContent = BinaryContent.create(
        "profile.png",
        100L,
        "image/png",
        "users/%s/profile/test.png".formatted(UUID.randomUUID())
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));

    // when
    binaryContentService.updatesStatusFail(binaryContentId);

    // then
    assertThat(binaryContent.getStatus()).isEqualTo(BinaryContentStatus.FAIL);
  }

  private BinaryContent saveWithId(BinaryContent binaryContent) {
    if (binaryContent.getId() == null) {
      ReflectionTestUtils.setField(binaryContent, "id", UUID.randomUUID());
    }
    return binaryContent;
  }

  private MockMultipartFile createImage(String fileName, String contentType) {
    return new MockMultipartFile(
        "image",
        fileName,
        contentType,
        "image-bytes".getBytes(StandardCharsets.UTF_8)
    );
  }

  private MockMultipartFile createEmptyImage(String fileName, String contentType) {
    return new MockMultipartFile(
        "image",
        fileName,
        contentType,
        new byte[0]
    );
  }

  private MultipartFile createZeroSizeImage(String fileName, String contentType) {
    MultipartFile image = mock(MultipartFile.class);
    given(image.isEmpty()).willReturn(false);
    given(image.getSize()).willReturn(0L);
    given(image.getOriginalFilename()).willReturn(fileName);
    given(image.getContentType()).willReturn(contentType);
    return image;
  }
}
