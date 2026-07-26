package com.team6.moduply.binarycontent.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
import com.team6.moduply.content.service.ContentImageUploadService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BinaryContentStorageEventListenerTest {

  @Mock
  private BinaryContentStorage binaryContentStorage;

  @Mock
  private BinaryContentService binaryContentService;

  @Mock
  private BinaryContentRepository binaryContentRepository;

  @Mock
  private ContentImageUploadService contentImageUploadService;

  @InjectMocks
  private BinaryContentStorageEventListener listener;

  @Test
  @DisplayName("파일 저장 이벤트 처리 시 저장소 업로드 후 SUCCESS 상태 변경을 요청한다.")
  void handleBinaryContentStorage_success() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    UUID oldBinaryContentId = UUID.randomUUID();
    String oldStorageKey = "contents/old/thumbnail/old.png";
    byte[] bytes = "image-bytes".getBytes();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        bytes,
        null,
        UUID.randomUUID(),
        oldBinaryContentId,
        oldStorageKey
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
    String imageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/image.png";
    given(binaryContentStorage.upload(
        "contents/content-id/thumbnail/image.png",
        bytes,
        "image/png"
    )).willReturn(imageUrl);

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verify(binaryContentStorage).upload("contents/content-id/thumbnail/image.png", bytes, "image/png");
    verify(contentImageUploadService).complete(
        event.getContentId(),
        binaryContentId,
        imageUrl,
        oldBinaryContentId,
        oldStorageKey
    );
    verify(contentImageUploadService).evictCaches(event.getContentId(), binaryContentId);
    verify(binaryContentService, never()).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("콘텐츠 이미지 캐시 제거에 실패해도 업로드한 객체와 SUCCESS 상태를 유지한다.")
  void handleBinaryContentStorage_success_when_cache_evict_fails() {
    UUID binaryContentId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    byte[] bytes = "image-bytes".getBytes();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        bytes,
        null,
        contentId,
        null,
        null
    );
    String imageUrl = "https://bucket/image.png";
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
    given(binaryContentStorage.upload(
        "contents/content-id/thumbnail/image.png",
        bytes,
        "image/png"
    )).willReturn(imageUrl);
    willThrow(new RuntimeException("redis unavailable"))
        .given(contentImageUploadService)
        .evictCaches(contentId, binaryContentId);

    listener.handleBinaryContentStorage(event);

    verify(contentImageUploadService).complete(
        contentId,
        binaryContentId,
        imageUrl,
        null,
        null
    );
    verify(binaryContentStorage, never()).delete(binaryContent.getStorageKey());
    verify(binaryContentService, never()).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 저장 이벤트 처리 중 업로드에 실패하면 FAIL 상태 변경을 요청한다.")
  void handleBinaryContentStorage_fail_when_upload_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    byte[] bytes = "image-bytes".getBytes();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        bytes,
        null,
        UUID.randomUUID(),
        null,
        null
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
    willThrow(new RuntimeException("upload failed"))
        .given(binaryContentStorage)
        .upload("contents/content-id/thumbnail/image.png", bytes, "image/png");

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verify(binaryContentStorage).delete("contents/content-id/thumbnail/image.png");
    verify(binaryContentService).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 저장 이벤트 처리 중 업로드 실패 후 보상 삭제도 실패하면 FAIL 상태 변경을 요청한다.")
  void handleBinaryContentStorage_fail_when_compensating_delete_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    byte[] bytes = "image-bytes".getBytes();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        bytes,
        null,
        UUID.randomUUID(),
        null,
        null
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
    willThrow(new RuntimeException("upload failed"))
        .given(binaryContentStorage)
        .upload("contents/content-id/thumbnail/image.png", bytes, "image/png");
    willThrow(new RuntimeException("delete failed"))
        .given(binaryContentStorage)
        .delete("contents/content-id/thumbnail/image.png");

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verify(binaryContentStorage).delete("contents/content-id/thumbnail/image.png");
    verify(binaryContentService).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 저장 이벤트 처리 중 업로드 실패 후 FAIL 상태 변경도 실패해도 예외를 전파하지 않는다.")
  void handleBinaryContentStorage_fail_when_fail_status_update_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    byte[] bytes = "image-bytes".getBytes();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        bytes,
        null,
        UUID.randomUUID(),
        null,
        null
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
    willThrow(new RuntimeException("upload failed"))
        .given(binaryContentStorage)
        .upload("contents/content-id/thumbnail/image.png", bytes, "image/png");
    willThrow(new RuntimeException("status update failed"))
        .given(binaryContentService)
        .updatesStatusFail(binaryContentId);

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verify(binaryContentStorage).delete("contents/content-id/thumbnail/image.png");
    verify(binaryContentService).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 저장 이벤트 처리 중 SUCCESS 상태 변경에 실패하면 업로드 파일 삭제를 요청한다.")
  void handleBinaryContentStorage_fail_when_success_status_update_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    byte[] bytes = "image-bytes".getBytes();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        bytes,
        null,
        UUID.randomUUID(),
        null,
        null
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.of(binaryContent));
    willThrow(new RuntimeException("status update failed"))
        .given(contentImageUploadService)
        .complete(event.getContentId(), binaryContentId, null, null, null);

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verify(binaryContentStorage).upload("contents/content-id/thumbnail/image.png", bytes, "image/png");
    verify(binaryContentStorage).delete("contents/content-id/thumbnail/image.png");
    verify(binaryContentService).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 저장 이벤트 처리 중 메타데이터 조회에 실패하면 상태 변경을 요청하지 않는다.")
  void handleBinaryContentStorage_fail_when_metadata_not_found() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        "image-bytes".getBytes(),
        null,
        UUID.randomUUID(),
        null,
        null
    );
    given(binaryContentRepository.findById(binaryContentId)).willReturn(Optional.empty());

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verifyNoInteractions(binaryContentStorage);
    verify(binaryContentService, never()).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 저장 이벤트 처리 중 메타데이터 조회 예외가 발생하면 FAIL 상태 변경을 요청한다.")
  void handleBinaryContentStorage_fail_when_metadata_lookup_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        "image-bytes".getBytes(),
        null,
        UUID.randomUUID(),
        null,
        null
    );
    given(binaryContentRepository.findById(binaryContentId))
        .willThrow(new RuntimeException("database error"));

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verifyNoInteractions(binaryContentStorage);
    verify(binaryContentService).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 저장 이벤트 처리 중 메타데이터 조회 예외 후 FAIL 상태 변경도 실패해도 예외를 전파하지 않는다.")
  void handleBinaryContentStorage_fail_when_metadata_lookup_and_fail_status_update_fail() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    BinaryContentCreatedEvent event = new BinaryContentCreatedEvent(
        binaryContentId,
        "image-bytes".getBytes(),
        null,
        UUID.randomUUID(),
        null,
        null
    );
    given(binaryContentRepository.findById(binaryContentId))
        .willThrow(new RuntimeException("database error"));
    willThrow(new RuntimeException("status update failed"))
        .given(binaryContentService)
        .updatesStatusFail(binaryContentId);

    // when
    listener.handleBinaryContentStorage(event);

    // then
    verifyNoInteractions(binaryContentStorage);
    verify(binaryContentService).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 삭제 이벤트 처리 시 저장소 삭제 후 DELETED 상태 변경을 요청한다.")
  void handleBinaryContentDelete_success() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    String storageKey = "contents/content-id/thumbnail/image.png";
    BinaryContentDeletedEvent event = new BinaryContentDeletedEvent(binaryContentId, storageKey);

    // when
    listener.handleBinaryContentDelete(event);

    // then
    verify(binaryContentStorage).delete(storageKey);
    verify(binaryContentService).updatesStatusDeleted(binaryContentId);
    verify(binaryContentService, never()).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("이미지 URL 캐시 제거에 실패해도 저장소 객체를 삭제한다.")
  void handleBinaryContentDelete_success_when_cache_evict_fails() {
    UUID binaryContentId = UUID.randomUUID();
    String storageKey = "contents/content-id/thumbnail/image.png";
    BinaryContentDeletedEvent event = new BinaryContentDeletedEvent(binaryContentId, storageKey);
    willThrow(new RuntimeException("redis unavailable"))
        .given(binaryContentService)
        .evictUrl(binaryContentId);

    listener.handleBinaryContentDelete(event);

    verify(binaryContentStorage).delete(storageKey);
    verify(binaryContentService).updatesStatusDeleted(binaryContentId);
  }

  @Test
  @DisplayName("파일 삭제 이벤트 처리 중 삭제 실패 후 FAIL 상태 변경도 실패해도 예외를 전파하지 않는다.")
  void handleBinaryContentDelete_fail_when_fail_status_update_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    String storageKey = "contents/content-id/thumbnail/image.png";
    BinaryContentDeletedEvent event = new BinaryContentDeletedEvent(binaryContentId, storageKey);
    willThrow(new RuntimeException("delete failed")).given(binaryContentStorage).delete(storageKey);
    willThrow(new RuntimeException("status update failed"))
        .given(binaryContentService)
        .updatesStatusFail(binaryContentId);

    // when
    listener.handleBinaryContentDelete(event);

    // then
    verify(binaryContentStorage).delete(storageKey);
    verify(binaryContentService).updatesStatusFail(binaryContentId);
    verify(binaryContentService, never()).updatesStatusDeleted(binaryContentId);
  }

  @Test
  @DisplayName("파일 삭제 이벤트 처리 중 DELETED 상태 변경에 실패해도 예외를 전파하지 않는다.")
  void handleBinaryContentDelete_fail_when_deleted_status_update_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    String storageKey = "contents/content-id/thumbnail/image.png";
    BinaryContentDeletedEvent event = new BinaryContentDeletedEvent(binaryContentId, storageKey);
    willThrow(new RuntimeException("status update failed"))
        .given(binaryContentService)
        .updatesStatusDeleted(binaryContentId);

    // when
    listener.handleBinaryContentDelete(event);

    // then
    verify(binaryContentStorage).delete(storageKey);
    verify(binaryContentService).updatesStatusDeleted(binaryContentId);
    verify(binaryContentService, never()).updatesStatusFail(binaryContentId);
  }

  @Test
  @DisplayName("파일 삭제 이벤트 처리 중 삭제에 실패하면 FAIL 상태 변경을 요청한다.")
  void handleBinaryContentDelete_fail_when_delete_fails() {
    // given
    UUID binaryContentId = UUID.randomUUID();
    String storageKey = "contents/content-id/thumbnail/image.png";
    BinaryContentDeletedEvent event = new BinaryContentDeletedEvent(binaryContentId, storageKey);
    willThrow(new RuntimeException("delete failed")).given(binaryContentStorage).delete(storageKey);

    // when
    listener.handleBinaryContentDelete(event);

    // then
    verify(binaryContentService).updatesStatusFail(binaryContentId);
    verify(binaryContentService, never()).updatesStatusDeleted(binaryContentId);
  }

  private BinaryContent createBinaryContent(UUID binaryContentId) {
    BinaryContent binaryContent = BinaryContent.create(
        "image.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/image.png"
    );
    ReflectionTestUtils.setField(binaryContent, "id", binaryContentId);

    return binaryContent;
  }
}
