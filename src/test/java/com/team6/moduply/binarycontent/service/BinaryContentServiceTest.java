package com.team6.moduply.binarycontent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.BinaryContentStatus;
import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.event.BinaryContentCreatedEvent;
import com.team6.moduply.binarycontent.event.BinaryContentDeletedEvent;
import com.team6.moduply.binarycontent.exception.BinaryContentErrorCode;
import com.team6.moduply.binarycontent.exception.BinaryContentException;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
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
  private BinaryContentStorage binaryContentStorage;

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

    // 기존 프로필 이미지 생성
    UUID oldBinaryContentId = UUID.randomUUID();
    String oldStorageKey = "users/%s/profile/old-profile.png".formatted(userId);
    BinaryContent oldProfileImg = BinaryContent.create(
            "old-profile.png",
            1000L,
            "image/png",
            oldStorageKey
    );
    ReflectionTestUtils.setField(
            oldProfileImg,
            "id",
            oldBinaryContentId
    );

    // binaryContent 저장
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> saveWithId(invocation.getArgument(0)));

    // when
    BinaryContent result = binaryContentService.createUserProfile(userId, image, oldProfileImg);

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
    assertThat(event.getOldBinaryContentId()).isEqualTo(oldBinaryContentId);
    assertThat(event.getOldStorageKey()).isEqualTo(oldStorageKey);
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
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, image, null))
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
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, null, null))
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
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, image, null))
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
    assertThatThrownBy(() -> binaryContentService.createUserProfile(userId, image, null))
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
    UUID oldBinaryContentId = UUID.randomUUID();
    String oldStorageKey = "contents/%s/thumbnail/old-thumbnail.webp".formatted(contentId);
    BinaryContent oldContentImg = BinaryContent.create(
        "old-thumbnail.webp",
        1000L,
        "image/webp",
        oldStorageKey
    );
    ReflectionTestUtils.setField(oldContentImg, "id", oldBinaryContentId);
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> saveWithId(invocation.getArgument(0)));

    // when
    BinaryContent result = binaryContentService.createContentImage(contentId, image, oldContentImg);

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
    assertThat(event.getOldBinaryContentId()).isEqualTo(oldBinaryContentId);
    assertThat(event.getOldStorageKey()).isEqualTo(oldStorageKey);
  }

  @Test
  @DisplayName("외부 이미지 bytes로 콘텐츠 이미지 생성 시 BinaryContent를 저장하고 업로드 이벤트를 발행한다.")
  void createContentImage_success_with_external_image_bytes() {
    // given
    UUID contentId = UUID.randomUUID();
    byte[] bytes = "external-image-bytes".getBytes(StandardCharsets.UTF_8);
    given(binaryContentRepository.save(any(BinaryContent.class)))
        .willAnswer(invocation -> saveWithId(invocation.getArgument(0)));

    // when
    BinaryContent result = binaryContentService.createContentImage(
        contentId,
        "tmdb-movie-27205.jpg",
        bytes,
        "image/jpeg",
        null
    );

    // then
    assertThat(result.getFileName()).isEqualTo("tmdb-movie-27205.jpg");
    assertThat(result.getSize()).isEqualTo((long) bytes.length);
    assertThat(result.getContentType()).isEqualTo("image/jpeg");
    assertThat(result.getStorageKey())
        .startsWith("contents/%s/thumbnail/".formatted(contentId))
        .endsWith(".jpg");
    assertThat(result.getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);

    ArgumentCaptor<BinaryContentCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(BinaryContentCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    BinaryContentCreatedEvent event = eventCaptor.getValue();
    assertThat(event.getBinaryContentId()).isEqualTo(result.getId());
    assertThat(event.getBytes()).isEqualTo(bytes);
    assertThat(event.getUserId()).isNull();
    assertThat(event.getContentId()).isEqualTo(contentId);
    assertThat(event.getOldBinaryContentId()).isNull();
    assertThat(event.getOldStorageKey()).isNull();
  }

  @Test
  @DisplayName("외부 이미지 bytes가 비어 있으면 EMPTY_FILE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createContentImage_fail_when_external_image_bytes_is_empty() {
    // given
    UUID contentId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(
        contentId,
        "tmdb-movie-27205.jpg",
        new byte[0],
        "image/jpeg",
        null
    ))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.EMPTY_FILE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("외부 이미지 타입이 지원되지 않으면 UNSUPPORTED_IMAGE_TYPE 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createContentImage_fail_when_external_image_type_is_unsupported() {
    // given
    UUID contentId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(
        contentId,
        "tmdb-movie-27205.gif",
        "external-image-bytes".getBytes(StandardCharsets.UTF_8),
        "image/gif",
        null
    ))
        .isInstanceOfSatisfying(BinaryContentException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(BinaryContentErrorCode.UNSUPPORTED_IMAGE_TYPE)
        );

    verify(binaryContentRepository, never()).save(any(BinaryContent.class));
    verify(eventPublisher, never()).publishEvent(any(BinaryContentCreatedEvent.class));
  }

  @Test
  @DisplayName("콘텐츠 이미지 생성 시 지원하지 않는 이미지 타입이면 예외가 발생하고 저장과 이벤트 발행을 하지 않는다.")
  void createContentImage_fail_when_unsupported_image_type() {
    // given
    UUID contentId = UUID.randomUUID();
    MockMultipartFile image = createImage("thumbnail.gif", "image/gif");

    // when & then
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, image, null))
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
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, null, null))
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
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, image, null))
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
    assertThatThrownBy(() -> binaryContentService.createContentImage(contentId, image, null))
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
    binaryContentService.updatesStatusSuccessAndPublishDeleteEvent(binaryContentId, null, null);

    // then
    assertThat(binaryContent.getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);
  }

  @Test
  @DisplayName("S3 업로드 성공 처리 시 BinaryContent가 존재하지 않으면 BINARY_CONTENT_NOT_FOUND 예외가 발생한다.")
  void updatesStatusSuccess_fail_when_binary_content_not_found() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> binaryContentService.updatesStatusSuccessAndPublishDeleteEvent(binaryContentId, null, null))
        .isInstanceOfSatisfying(BinaryContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("binaryContentId")).isEqualTo(binaryContentId);
        });
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

  @Test
  @DisplayName("S3 업로드 실패 처리 시 BinaryContent가 존재하지 않으면 BINARY_CONTENT_NOT_FOUND 예외가 발생한다.")
  void updatesStatusFail_fail_when_binary_content_not_found() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> binaryContentService.updatesStatusFail(binaryContentId))
        .isInstanceOfSatisfying(BinaryContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("binaryContentId")).isEqualTo(binaryContentId);
        });
  }

  @Test
  @DisplayName("BinaryContent가 null이면 URL을 생성하지 않고 null을 반환한다.")
  void generateUrl_return_null_when_binary_content_is_null() {
    // when
    String result = binaryContentService.generateUrl(null);

    // then
    assertThat(result).isNull();
    verify(binaryContentStorage, never()).generateUrl(any(String.class), any(String.class));
  }

  @Test
  @DisplayName("BinaryContent가 있으면 storageKey와 contentType으로 URL을 생성한다.")
  void generateUrl_success() {
    // given
    BinaryContent binaryContent = BinaryContent.create(
        "profile.png",
        100L,
        "image/png",
        "users/user-id/profile/profile.png"
    );
    given(binaryContentStorage.generateUrl(
        eq("users/user-id/profile/profile.png"),
        eq("image/png")
    )).willReturn("https://example.com/profile.png");

    // when
    String result = binaryContentService.generateUrl(binaryContent);

    // then
    assertThat(result).isEqualTo("https://example.com/profile.png");
    verify(binaryContentStorage).generateUrl(
        "users/user-id/profile/profile.png",
        "image/png"
    );
  }

  @Test
  @DisplayName("BinaryContent 삭제 요청 시 삭제 이벤트를 발행한다.")
  void delete_success_publish_event() {
    // given
    // 삭제할 binaryContentId
    UUID binaryContentId = UUID.randomUUID();
    // 삭제할 이미지의 S3경로
    String storageKey = "users/user-id/profile/profile.png";
    BinaryContent binaryContent = BinaryContent.create(
        "profile.png",
        100L,
        "image/png",
        storageKey
    );
    // 위에서만든 binaryContentId값을 binaryContent에 강제 주입
    ReflectionTestUtils.setField(binaryContent, "id", binaryContentId);
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));

    // when
    binaryContentService.delete(binaryContentId);

    // then
    // eventPublisher.publishEvnet(???) -> ???에 해당하는 인자를 가로채서 가져온다.
    ArgumentCaptor<BinaryContentDeletedEvent> eventCaptor =
        ArgumentCaptor.forClass(BinaryContentDeletedEvent.class);
    // 이벤트발행 됐는지?
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    // BinaryContentDeletedEvent 객체를 가져오게 됨.
    BinaryContentDeletedEvent event = eventCaptor.getValue();
    assertThat(event.getBinaryContentId()).isEqualTo(binaryContentId);
    assertThat(event.getStorageKey()).isEqualTo(storageKey);
  }

  @Test
  @DisplayName("BinaryContent 삭제 요청 시 대상이 없으면 BINARY_CONTENT_NOT_FOUND 예외가 발생하고 이벤트를 발행하지 않는다.")
  void delete_fail_when_binary_content_not_found() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> binaryContentService.delete(binaryContentId))
        .isInstanceOfSatisfying(BinaryContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(BinaryContentErrorCode.BINARY_CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("binaryContentId")).isEqualTo(binaryContentId);
        });

    verify(eventPublisher, never()).publishEvent(any(BinaryContentDeletedEvent.class));
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
    return image;
  }
}
