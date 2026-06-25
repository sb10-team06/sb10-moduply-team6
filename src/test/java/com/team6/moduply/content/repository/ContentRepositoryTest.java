package com.team6.moduply.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.binarycontent.BinaryContentStatus;
import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@Import(JpaAuditingConfig.class)
class ContentRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private ContentRepository contentRepository;

  @Test
  @DisplayName("콘텐츠 ID로 콘텐츠 이미지와 함께 조회한다.")
  void find_by_id_with_content_img_success_with_existing_content() {
    // Given
    BinaryContent contentImg = createBinaryContent();
    Content content = new Content(
        contentImg,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    Content savedContent = contentRepository.save(content);

    // When
    Optional<Content> result = contentRepository.findByIdWithContentImg(savedContent.getId());

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedContent.getId());
    assertThat(result.get().getContentImg()).isNotNull();
    assertThat(result.get().getContentImg().getStorageKey()).isEqualTo("contents/images/thumbnail.jpg");
  }

  private BinaryContent createBinaryContent() {
    BinaryContent contentImg = new BinaryContent();
    ReflectionTestUtils.setField(contentImg, "fileName", "thumbnail.jpg");
    ReflectionTestUtils.setField(contentImg, "size", 1024L);
    ReflectionTestUtils.setField(contentImg, "contentType", "image/jpeg");
    ReflectionTestUtils.setField(contentImg, "storageKey", "contents/images/thumbnail.jpg");
    ReflectionTestUtils.setField(contentImg, "status", BinaryContentStatus.SUCCESS);

    return contentImg;
  }
}
