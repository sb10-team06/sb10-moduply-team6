package com.team6.moduply.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.exception.ContentException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@Import(JpaAuditingConfig.class)
class ContentRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private TagRepository tagRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

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

  @Test
  @DisplayName("콘텐츠 목록을 인기순으로 조회하고 커서 이후 목록을 조회한다.")
  void find_contents_success_with_watcher_count_sort_and_cursor() {
    // Given
    Content first = contentRepository.save(createContent(
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화",
        100L
    ));
    Content second = contentRepository.save(createContent(
        ContentType.movie,
        "Interstellar",
        "우주 SF 영화",
        50L
    ));

    // When
    List<Content> firstPage = contentRepository.findContents(
        null,
        null,
        List.of(),
        null,
        null,
        2,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );
    List<Content> secondPage = contentRepository.findContents(
        null,
        null,
        List.of(),
        String.valueOf(first.getWatcherCount()),
        first.getId(),
        2,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(firstPage)
        .extracting(Content::getId)
        .containsExactly(first.getId(), second.getId());
    assertThat(secondPage)
        .extracting(Content::getId)
        .containsExactly(second.getId());
  }

  @Test
  @DisplayName("커서 값만 있고 보조 커서가 없으면 콘텐츠 목록 조회에 실패한다.")
  void find_contents_fail_when_cursor_without_id_after() {
    // Given
    String cursor = Instant.now().toString();

    // When & Then
    assertThatThrownBy(() -> contentRepository.findContents(
        null,
        null,
        null,
        cursor,
        null,
        10,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    )).isInstanceOf(ContentException.class);
  }

  @Test
  @DisplayName("생성일 커서 형식이 올바르지 않으면 콘텐츠 목록 조회에 실패한다.")
  void find_contents_fail_when_created_at_cursor_invalid() {
    // Given
    String invalidCursor = "invalid-cursor";
    UUID idAfter = UUID.randomUUID();

    // When & Then
    assertThatThrownBy(() -> contentRepository.findContents(
        null,
        null,
        null,
        invalidCursor,
        idAfter,
        10,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    )).isInstanceOf(ContentException.class);
  }

  @Test
  @DisplayName("콘텐츠 목록을 평점순으로 조회한다.")
  void find_contents_success_with_rate_sort() {
    // Given
    Content highRated = contentRepository.save(createContent(
        ContentType.movie,
        "High Rated Movie",
        "평점이 높은 영화",
        100L,
        BigDecimal.valueOf(4.50)
    ));
    Content lowRated = contentRepository.save(createContent(
        ContentType.movie,
        "Low Rated Movie",
        "평점이 낮은 영화",
        50L,
        BigDecimal.valueOf(3.20)
    ));

    // When
    List<Content> result = contentRepository.findContents(
        null,
        null,
        List.of(),
        null,
        null,
        10,
        ContentSortBy.rate,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(result)
        .extracting(Content::getId)
        .containsExactly(highRated.getId(), lowRated.getId());
  }

  @Test
  @DisplayName("콘텐츠 목록을 최신순 오름차순으로 조회한다.")
  void find_contents_success_with_created_at_ascending_sort() {
    // Given
    Content first = contentRepository.save(createContent(
        ContentType.movie,
        "Old Movie",
        "먼저 등록된 영화",
        100L
    ));
    Content second = contentRepository.save(createContent(
        ContentType.movie,
        "New Movie",
        "나중에 등록된 영화",
        50L
    ));

    // When
    List<Content> result = contentRepository.findContents(
        null,
        null,
        List.of(),
        null,
        null,
        10,
        ContentSortBy.createdAt,
        SortDirection.ASCENDING
    );

    // Then
    assertThat(result)
        .extracting(Content::getId)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  @DisplayName("동일한 정렬값이면 ID 보조 커서로 다음 목록을 조회한다.")
  void find_contents_success_with_id_after_when_sort_values_are_same() {
    // Given
    Content first = contentRepository.save(createContent(
        ContentType.movie,
        "First Movie",
        "시청자 수가 같은 영화",
        100L
    ));
    Content second = contentRepository.save(createContent(
        ContentType.movie,
        "Second Movie",
        "시청자 수가 같은 영화",
        100L
    ));
    Content third = contentRepository.save(createContent(
        ContentType.movie,
        "Third Movie",
        "시청자 수가 같은 영화",
        100L
    ));
    List<UUID> allIds = List.of(first.getId(), second.getId(), third.getId());

    // When
    List<Content> firstPage = contentRepository.findContents(
        null,
        null,
        List.of(),
        null,
        null,
        1,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );
    UUID idAfter = firstPage.get(0).getId();
    List<Content> secondPage = contentRepository.findContents(
        null,
        null,
        List.of(),
        "100",
        idAfter,
        2,
        ContentSortBy.watcherCount,
        SortDirection.DESCENDING
    );

    // Then
    assertThat(firstPage)
        .extracting(Content::getId)
        .hasSize(1);
    assertThat(secondPage)
        .extracting(Content::getId)
        .hasSize(2)
        .doesNotContain(idAfter)
        .isSubsetOf(allIds);
  }

  @Test
  @DisplayName("콘텐츠 목록을 타입, 검색어, 태그 조건으로 조회한다.")
  void find_contents_success_with_type_keyword_and_tags() {
    // Given
    Content matched = contentRepository.save(createContent(
        ContentType.movie,
        "Dream Movie",
        "꿈을 다루는 영화",
        100L
    ));
    Content unmatched = contentRepository.save(createContent(
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠",
        50L
    ));
    Tag sf = tagRepository.save(new Tag("SF"));
    Tag sports = tagRepository.save(new Tag("스포츠"));
    contentTagRepository.saveAll(List.of(
        new ContentTag(matched, sf),
        new ContentTag(unmatched, sports)
    ));

    // When
    List<Content> result = contentRepository.findContents(
        ContentType.movie,
        "dream",
        List.of("SF"),
        null,
        null,
        10,
        ContentSortBy.createdAt,
        SortDirection.DESCENDING
    );
    long totalCount = contentRepository.countContents(
        ContentType.movie,
        "dream",
        List.of("SF")
    );

    // Then
    assertThat(result)
        .extracting(Content::getId)
        .containsExactly(matched.getId());
    assertThat(totalCount).isEqualTo(1);
  }

  private BinaryContent createBinaryContent() {
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.jpg",
        1024L,
        "image/jpeg",
        "contents/images/thumbnail.jpg"
    );
    contentImg.success();

    return contentImg;
  }

  private Content createContent(
      ContentType type,
      String title,
      String description,
      long watcherCount
  ) {
    Content content = new Content(
        null,
        null,
        type,
        title,
        description
    );
    ReflectionTestUtils.setField(content, "watcherCount", watcherCount);

    return content;
  }

  private Content createContent(
      ContentType type,
      String title,
      String description,
      long watcherCount,
      BigDecimal averageRating
  ) {
    Content content = createContent(type, title, description, watcherCount);
    ReflectionTestUtils.setField(content, "averageRating", averageRating);

    return content;
  }
}
