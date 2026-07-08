package com.team6.moduply.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.content.entity.Tag;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaAuditingConfig.class)
class TagRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TagRepository tagRepository;

  @Test
  @DisplayName("태그 이름 목록으로 기존 태그를 조회한다.")
  void find_all_by_tag_name_in_success_with_existing_tag_names() {
    // Given
    Tag sf = new Tag("SF");
    Tag action = new Tag("액션");
    Tag drama = new Tag("드라마");
    tagRepository.saveAll(List.of(sf, action, drama));

    // When
    List<Tag> result = tagRepository.findAllByTagNameIn(List.of("SF", "액션", "없는태그"));

    // Then
    assertThat(result)
        .extracting(Tag::getTagName)
        .containsExactlyInAnyOrder("SF", "액션");
  }

  @Test
  @DisplayName("이미 존재하는 태그 이름이면 중복 저장 없이 무시한다.")
  void insert_ignore_success_when_tag_name_already_exists() {
    // Given
    tagRepository.save(new Tag("SF"));

    // When
    int result = tagRepository.insertIgnore(UUID.randomUUID(), "SF");
    List<Tag> tags = tagRepository.findAllByTagNameIn(List.of("SF"));

    // Then
    assertThat(result).isZero();
    assertThat(tags).hasSize(1);
  }

  @Test
  @DisplayName("존재하지 않는 태그 이름이면 신규 저장한다.")
  void insert_ignore_success_when_tag_name_does_not_exist() {
    // When
    int result = tagRepository.insertIgnore(UUID.randomUUID(), "SF");
    List<Tag> tags = tagRepository.findAllByTagNameIn(List.of("SF"));

    // Then
    assertThat(result).isOne();
    assertThat(tags)
        .extracting(Tag::getTagName)
        .containsExactly("SF");
  }
}
