package com.team6.moduply.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.binarycontent.service.BinaryContentService;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.dto.ContentListCachePageDto;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.ContentTagRepository.ContentTagNameProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentListCacheServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  @Mock
  private BinaryContentService binaryContentService;

  @InjectMocks
  private ContentListCacheService contentListCacheService;

  @Test
  @DisplayName("캐시 목록 조회 시 콘텐츠, 이미지 URL, 태그 정보를 캐시 DTO로 변환한다.")
  void find_created_at_page_success_with_contents_and_has_next() {
    // Given
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();
    UUID thirdId = UUID.randomUUID();
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.jpg",
        100L,
        "image/jpeg",
        "contents/images/thumbnail.jpg"
    );
    Content first = createContent(firstId, contentImg, "Inception", "꿈과 현실", BigDecimal.valueOf(4.5), 10);
    Content second = createContent(secondId, null, "Interstellar", "우주 여행", BigDecimal.valueOf(3.5), 5);
    Content third = createContent(thirdId, null, "Tenet", "시간 역행", BigDecimal.ZERO, 0);

    given(contentRepository.findAllByCursor(
        ContentType.movie,
        "space",
        List.of("SF"),
        null,
        null,
        3,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    )).willReturn(List.of(first, second, third));
    given(contentTagRepository.findTagNamesByContentIds(List.of(firstId, secondId)))
        .willReturn(List.of(
            projection(firstId, "SF"),
            projection(firstId, "명작"),
            projection(secondId, "우주")
        ));
    given(binaryContentService.generateUrl(contentImg)).willReturn("https://example.com/thumbnail.jpg");
    given(contentRepository.countContents(ContentType.movie, "space", List.of("SF"))).willReturn(3L);

    // When
    ContentListCachePageDto result = contentListCacheService.findCreatedAtPage(
        ContentType.movie,
        "space",
        List.of("SF"),
        null,
        null,
        2,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result.data()).hasSize(2);
    assertThat(result.data().get(0).id()).isEqualTo(firstId);
    assertThat(result.data().get(0).thumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
    assertThat(result.data().get(0).tags()).containsExactly("SF", "명작");
    assertThat(result.data().get(0).averageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    assertThat(result.data().get(0).reviewCount()).isEqualTo(10);
    assertThat(result.data().get(1).thumbnailUrl()).isEqualTo("/placeholder-movie.png");
    assertThat(result.data().get(1).tags()).containsExactly("우주");
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(second.getCreatedAt().toString());
    assertThat(result.nextIdAfter()).isEqualTo(secondId);
    assertThat(result.totalCount()).isEqualTo(3L);
    assertThat(result.sortBy()).isEqualTo(ContentSortBy.createdAt.name());
    assertThat(result.sortDirection()).isEqualTo(SortDirection.DESCENDING);
  }

  @Test
  @DisplayName("첫 페이지가 마지막 페이지이면 현재 페이지 크기를 전체 개수로 사용한다.")
  void find_created_at_page_success_with_first_and_last_page() {
    // Given
    UUID contentId = UUID.randomUUID();
    Content content = createContent(contentId, null, "Inception", "꿈과 현실", BigDecimal.ZERO, 0);
    given(contentRepository.findAllByCursor(
        null,
        null,
        List.of(),
        null,
        null,
        11,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    )).willReturn(List.of(content));
    given(contentTagRepository.findTagNamesByContentIds(List.of(contentId))).willReturn(List.of());

    // When
    ContentListCachePageDto result = contentListCacheService.findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        10,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result.data()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.totalCount()).isEqualTo(1L);
    verify(contentRepository, never()).countContents(null, null, List.of());
  }

  @Test
  @DisplayName("인기순 목록 조회 시 시청자 수를 다음 커서로 사용한다.")
  void find_created_at_page_success_with_watcher_count_cursor() {
    // Given
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();
    UUID thirdId = UUID.randomUUID();
    Content first = createContent(firstId, null, "Low", "낮은 시청자 수", BigDecimal.ZERO, 0, 10L);
    Content second = createContent(secondId, null, "Middle", "중간 시청자 수", BigDecimal.ZERO, 0, 20L);
    Content third = createContent(thirdId, null, "High", "높은 시청자 수", BigDecimal.ZERO, 0, 30L);

    given(contentRepository.findAllByCursor(
        null,
        null,
        List.of(),
        null,
        null,
        3,
        ContentSortBy.watcherCount,
        SortDirection.ASCENDING
    )).willReturn(List.of(first, second, third));
    given(contentTagRepository.findTagNamesByContentIds(List.of(firstId, secondId))).willReturn(List.of());
    given(contentRepository.countContents(null, null, List.of())).willReturn(3L);

    // When
    ContentListCachePageDto result = contentListCacheService.findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        2,
        ContentSortBy.watcherCount,
        SortDirection.ASCENDING
    );

    // Then
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo("20");
    assertThat(result.nextIdAfter()).isEqualTo(secondId);
  }

  @Test
  @DisplayName("평점순 목록 조회 시 평점을 다음 커서로 사용한다.")
  void find_created_at_page_success_with_rate_cursor() {
    // Given
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();
    UUID thirdId = UUID.randomUUID();
    Content first = createContent(firstId, null, "High", "높은 평점", new BigDecimal("4.50"), 1);
    Content second = createContent(secondId, null, "Middle", "중간 평점", new BigDecimal("3.50"), 1);
    Content third = createContent(thirdId, null, "Low", "낮은 평점", new BigDecimal("2.50"), 1);

    given(contentRepository.findAllByCursor(
        null,
        null,
        List.of(),
        null,
        null,
        3,
        ContentSortBy.rate,
        SortDirection.DESCENDING
    )).willReturn(List.of(first, second, third));
    given(contentTagRepository.findTagNamesByContentIds(List.of(firstId, secondId))).willReturn(List.of());
    given(contentRepository.countContents(null, null, List.of())).willReturn(3L);

    // When
    ContentListCachePageDto result = contentListCacheService.findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        2,
        ContentSortBy.rate,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo("3.50");
    assertThat(result.nextIdAfter()).isEqualTo(secondId);
  }

  @Test
  @DisplayName("조회 결과가 비어 있으면 태그 조회 없이 빈 페이지를 반환한다.")
  void find_created_at_page_success_with_empty_contents() {
    // Given
    given(contentRepository.findAllByCursor(
        null,
        null,
        List.of(),
        null,
        null,
        11,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    )).willReturn(List.of());

    // When
    ContentListCachePageDto result = contentListCacheService.findCreatedAtPage(
        null,
        null,
        List.of(),
        null,
        null,
        10,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result.data()).isEmpty();
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isZero();
    verify(contentTagRepository, never()).findTagNamesByContentIds(anyList());
  }

  private Content createContent(
      UUID contentId,
      BinaryContent contentImg,
      String title,
      String description,
      BigDecimal averageRating,
      int reviewCount
  ) {
    return createContent(contentId, contentImg, title, description, averageRating, reviewCount, 0L);
  }

  private Content createContent(
      UUID contentId,
      BinaryContent contentImg,
      String title,
      String description,
      BigDecimal averageRating,
      int reviewCount,
      long watcherCount
  ) {
    Content content = new Content(
        contentImg,
        null,
        ContentType.movie,
        title,
        description
    );
    ReflectionTestUtils.setField(content, "id", contentId);
    ReflectionTestUtils.setField(content, "averageRating", averageRating);
    ReflectionTestUtils.setField(content, "reviewCount", reviewCount);
    ReflectionTestUtils.setField(content, "watcherCount", watcherCount);
    ReflectionTestUtils.setField(content, "createdAt", Instant.now());
    ReflectionTestUtils.setField(content, "updatedAt", Instant.now());

    return content;
  }

  private ContentTagNameProjection projection(UUID contentId, String tagName) {
    return new ContentTagNameProjection() {
      @Override
      public UUID getContentId() {
        return contentId;
      }

      @Override
      public String getTagName() {
        return tagName;
      }
    };
  }
}
