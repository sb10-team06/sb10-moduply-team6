package com.team6.moduply.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.BinaryContentStatus;
import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.event.BinaryContentDeletedEvent;
import com.team6.moduply.binarycontent.repository.BinaryContentRepository;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.repository.ContentRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentImageUploadServiceTest {

  @Mock
  private BinaryContentRepository binaryContentRepository;
  @Mock
  private ContentRepository contentRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @InjectMocks
  private ContentImageUploadService service;

  @Test
  @DisplayName("콘텐츠 이미지 업로드 완료 시 DB URL과 상태를 갱신하고 기존 이미지 삭제 이벤트를 발행한다.")
  void complete_success_with_uploaded_image_url() {
    UUID contentId = UUID.randomUUID();
    UUID binaryContentId = UUID.randomUUID();
    UUID oldBinaryContentId = UUID.randomUUID();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    Content content = createContent(createBinaryContent(oldBinaryContentId));
    String imageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/thumbnail.png";
    given(binaryContentRepository.findById(binaryContentId))
        .willReturn(Optional.of(binaryContent));
    given(contentRepository.findByIdWithContentImgForUpdate(contentId))
        .willReturn(Optional.of(content));

    service.complete(
        contentId,
        binaryContentId,
        imageUrl,
        oldBinaryContentId,
        "contents/old-thumbnail.png"
    );

    assertThat(content.getThumbnailUrl()).isEqualTo(imageUrl);
    assertThat(content.getContentImg()).isSameAs(binaryContent);
    assertThat(binaryContent.getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);
    ArgumentCaptor<BinaryContentDeletedEvent> eventCaptor =
        ArgumentCaptor.forClass(BinaryContentDeletedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getBinaryContentId()).isEqualTo(oldBinaryContentId);
  }

  @Test
  @DisplayName("업로드한 이미지가 현재 콘텐츠 이미지와 다르면 URL 갱신에 실패한다.")
  void complete_fail_when_binary_content_does_not_match() {
    UUID contentId = UUID.randomUUID();
    UUID binaryContentId = UUID.randomUUID();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    Content content = createContent(createBinaryContent(UUID.randomUUID()));
    given(binaryContentRepository.findById(binaryContentId))
        .willReturn(Optional.of(binaryContent));
    given(contentRepository.findByIdWithContentImgForUpdate(contentId))
        .willReturn(Optional.of(content));

    assertThatThrownBy(() -> service.complete(
        contentId,
        binaryContentId,
        "https://bucket/thumbnail.png",
        null,
        null
    ))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode())
              .isEqualTo(ContentErrorCode.CONTENT_IMAGE_UPDATE_CONFLICT);
          assertThat(exception.getDetails()).containsEntry("contentId", contentId);
        });

    assertThat(binaryContent.getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);
    assertThat(content.getThumbnailUrl()).isNull();
    verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("이미지 업로드 완료 대상 콘텐츠가 없으면 콘텐츠 없음 예외가 발생한다.")
  void complete_fail_when_content_not_found() {
    UUID contentId = UUID.randomUUID();
    UUID binaryContentId = UUID.randomUUID();
    BinaryContent binaryContent = createBinaryContent(binaryContentId);
    given(binaryContentRepository.findById(binaryContentId))
        .willReturn(Optional.of(binaryContent));
    given(contentRepository.findByIdWithContentImgForUpdate(contentId))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> service.complete(
        contentId,
        binaryContentId,
        "https://bucket/thumbnail.png",
        null,
        null
    ))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
          assertThat(exception.getDetails()).containsEntry("contentId", contentId);
        });

    assertThat(binaryContent.getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);
    verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }

  private BinaryContent createBinaryContent(UUID id) {
    BinaryContent binaryContent = BinaryContent.create(
        "thumbnail.png",
        100L,
        "image/png",
        "contents/content-id/thumbnail/thumbnail.png"
    );
    ReflectionTestUtils.setField(binaryContent, "id", id);
    return binaryContent;
  }

  private Content createContent(BinaryContent binaryContent) {
    return new Content(
        binaryContent,
        null,
        ContentType.movie,
        "콘텐츠",
        "설명"
    );
  }
}
