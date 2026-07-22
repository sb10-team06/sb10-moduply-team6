package com.team6.moduply.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.content.dto.ContentDetailCacheDto;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import java.math.BigDecimal;
import java.util.List;
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
class ContentDetailCacheServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  @Mock
  private BinaryContentService binaryContentService;

  @InjectMocks
  private ContentDetailCacheService contentDetailCacheService;

  @Test
  @DisplayName("콘텐츠가 존재하면 현재 시청자 수를 제외한 단건 조회 캐시 값을 반환한다.")
  void find_success_with_existing_content() {
    // Given
    UUID contentId = UUID.randomUUID();
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.jpg",
        1024L,
        "image/jpeg",
        "contents/images/thumbnail.jpg"
    );
    Content content = new Content(
        contentImg,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ReflectionTestUtils.setField(content, "averageRating", BigDecimal.valueOf(4.5));
    ReflectionTestUtils.setField(content, "reviewCount", 10);
    List<String> tagNames = List.of("SF", "액션");

    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.of(content));
    given(binaryContentService.generateUrl(contentImg)).willReturn("https://example.com/thumbnail.jpg");
    given(contentTagRepository.findTagNamesByContentId(contentId)).willReturn(tagNames);

    // When
    ContentDetailCacheDto result = contentDetailCacheService.find(contentId);

    // Then
    assertThat(result.id()).isEqualTo(contentId);
    assertThat(result.type()).isEqualTo(ContentType.movie);
    assertThat(result.title()).isEqualTo("Inception");
    assertThat(result.description()).isEqualTo("꿈과 현실을 넘나드는 SF 영화");
    assertThat(result.thumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
    assertThat(result.tags()).containsExactly("SF", "액션");
    assertThat(result.averageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    assertThat(result.reviewCount()).isEqualTo(10);
    verify(contentRepository).findByIdWithContentImg(contentId);
    verify(binaryContentService).generateUrl(contentImg);
    verify(contentTagRepository).findTagNamesByContentId(contentId);
  }

  @Test
  @DisplayName("콘텐츠가 존재하지 않으면 예외를 던진다.")
  void find_fail_when_content_not_found() {
    // Given
    UUID contentId = UUID.randomUUID();
    given(contentRepository.findByIdWithContentImg(contentId)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> contentDetailCacheService.find(contentId))
        .isInstanceOfSatisfying(ContentException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
          assertThat(exception.getDetails().get("contentId")).isEqualTo(contentId);
        });
  }
}
