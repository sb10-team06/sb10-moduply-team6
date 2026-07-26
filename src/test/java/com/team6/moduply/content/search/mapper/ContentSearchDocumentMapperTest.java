package com.team6.moduply.content.search.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.search.document.ContentSearchDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ContentSearchDocumentMapperTest {

  private final ContentSearchDocumentMapper mapper = new ContentSearchDocumentMapper();

  @Test
  @DisplayName("Content와 태그명을 콘텐츠 검색 Document로 변환한다.")
  void toDocument_success() {
    // Given
    UUID contentId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-07-20T01:00:00Z");
    Instant updatedAt = Instant.parse("2026-07-20T02:00:00Z");
    Content content = new Content(
        null,
        "tmdb:movie:27205",
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 영화"
    );
    content.updateReviewStats(BigDecimal.valueOf(4.25), 12);
    ReflectionTestUtils.setField(content, "id", contentId);
    ReflectionTestUtils.setField(content, "createdAt", createdAt);
    ReflectionTestUtils.setField(content, "updatedAt", updatedAt);

    // When
    ContentSearchDocument document = mapper.toDocument(content, List.of("SF", "액션"));

    // Then
    assertThat(document.getId()).isEqualTo(contentId.toString());
    assertThat(document.getExternalApiId()).isEqualTo("tmdb:movie:27205");
    assertThat(document.getType()).isEqualTo(ContentType.movie);
    assertThat(document.getTitle()).isEqualTo("Inception");
    assertThat(document.getDescription()).isEqualTo("꿈과 현실을 넘나드는 영화");
    assertThat(document.getTags()).containsExactly("SF", "액션");
    assertThat(document.getAverageRating()).isEqualByComparingTo("4.25");
    assertThat(document.getReviewCount()).isEqualTo(12);
    assertThat(document.getCreatedAt()).isEqualTo(createdAt);
    assertThat(document.getUpdatedAt()).isEqualTo(updatedAt);
  }
}
