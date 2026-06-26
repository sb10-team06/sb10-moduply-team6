package com.team6.moduply.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentTagRepository.ContentTagNameProjection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaAuditingConfig.class)
class ContentTagRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private TagRepository tagRepository;

  @Autowired
  private ContentTagRepository contentTagRepository;

  @Test
  @DisplayName("콘텐츠 ID로 연결된 태그 이름 목록을 조회한다.")
  void find_tag_names_by_content_id_success_with_existing_content_tags() {
    // Given
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    );
    Content savedContent = contentRepository.save(content);

    Tag sf = tagRepository.save(new Tag("SF"));
    Tag action = tagRepository.save(new Tag("액션"));
    contentTagRepository.saveAll(List.of(
        new ContentTag(savedContent, action),
        new ContentTag(savedContent, sf)
    ));

    // When
    List<String> result = contentTagRepository.findTagNamesByContentId(savedContent.getId());

    // Then
    assertThat(result).containsExactly("SF", "액션");
  }

  @Test
  @DisplayName("콘텐츠 ID 목록으로 연결된 태그 이름 목록을 조회한다.")
  void find_tag_names_by_content_ids_success_with_existing_content_tags() {
    // Given
    Content movie = contentRepository.save(new Content(
        null,
        null,
        ContentType.movie,
        "Inception",
        "꿈과 현실을 넘나드는 SF 영화"
    ));
    Content sport = contentRepository.save(new Content(
        null,
        null,
        ContentType.sport,
        "World Cup",
        "스포츠 콘텐츠"
    ));

    Tag sf = tagRepository.save(new Tag("SF"));
    Tag action = tagRepository.save(new Tag("액션"));
    Tag sports = tagRepository.save(new Tag("스포츠"));
    contentTagRepository.saveAll(List.of(
        new ContentTag(movie, sf),
        new ContentTag(movie, action),
        new ContentTag(sport, sports)
    ));

    // When
    List<ContentTagNameProjection> result = contentTagRepository.findTagNamesByContentIds(List.of(
        movie.getId(),
        sport.getId()
    ));

    // Then
    assertThat(result)
        .extracting(ContentTagNameProjection::getContentId, ContentTagNameProjection::getTagName)
        .containsExactlyInAnyOrder(
            tuple(movie.getId(), "SF"),
            tuple(movie.getId(), "액션"),
            tuple(sport.getId(), "스포츠")
        );
  }
}
