package com.team6.moduply.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.enums.ContentType;
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
}
