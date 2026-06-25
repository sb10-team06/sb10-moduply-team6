package com.team6.moduply.content.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.enums.ContentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentDtoTest {

  @Test
  @DisplayName("콘텐츠 조회 응답 객체를 생성하면 필드 값이 유지된다.")
  void create_success_with_content_dto_fields() {
    // Given
    UUID contentId = UUID.randomUUID();
    List<String> tags = List.of("SF", "액션");

    // When
    ContentDto response = new ContentDto(
        contentId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "https://example.com/thumbnail.jpg",
        tags,
        BigDecimal.valueOf(4.5),
        10,
        100L
    );

    // Then
    assertThat(response.id()).isEqualTo(contentId);
    assertThat(response.type()).isEqualTo(ContentType.movie);
    assertThat(response.title()).isEqualTo("Inception");
    assertThat(response.description()).isEqualTo("꿈과 현실을 넘나드는 SF 영화");
    assertThat(response.thumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
    assertThat(response.tags()).containsExactly("SF", "액션");
    assertThat(response.averageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    assertThat(response.reviewCount()).isEqualTo(10);
    assertThat(response.watcherCount()).isEqualTo(100L);
  }

  @Test
  @DisplayName("공통 커서 페이지 응답 객체를 생성하면 페이지 정보가 유지된다.")
  void create_success_with_cursor_response_fields() {
    // Given
    UUID contentId = UUID.randomUUID();
    UUID nextIdAfter = UUID.randomUUID();
    ContentDto content = new ContentDto(
        contentId,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        "https://example.com/thumbnail.jpg",
        List.of("SF", "액션"),
        BigDecimal.valueOf(4.5),
        10,
        100L
    );

    // When
    CursorResponse<ContentDto> response = new CursorResponse<>(
        List.of(content),
        "2026-06-24T00:00:00Z",
        nextIdAfter,
        true,
        20L,
        "createdAt",
        SortDirection.DESCENDING
    );

    // Then
    assertThat(response.data()).containsExactly(content);
    assertThat(response.nextCursor()).isEqualTo("2026-06-24T00:00:00Z");
    assertThat(response.nextIdAfter()).isEqualTo(nextIdAfter);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.totalCount()).isEqualTo(20L);
    assertThat(response.sortBy()).isEqualTo("createdAt");
    assertThat(response.sortDirection()).isEqualTo(SortDirection.DESCENDING);
  }
}
