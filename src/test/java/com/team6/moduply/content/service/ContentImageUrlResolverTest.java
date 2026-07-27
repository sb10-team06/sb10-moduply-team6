package com.team6.moduply.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentImageUrlResolverTest {

  @Test
  @DisplayName("콘텐츠에 썸네일 URL이 있으면 저장된 URL을 반환한다.")
  void resolve_success_with_thumbnail_url() {
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "테스트 콘텐츠",
        "설명"
    );
    content.updateContentImage(null, "https://example.com/thumbnail.jpg");

    String result = ContentImageUrlResolver.resolve(content);

    assertThat(result).isEqualTo("https://example.com/thumbnail.jpg");
  }

  @Test
  @DisplayName("콘텐츠에 썸네일 URL이 없으면 기본 이미지 URL을 반환한다.")
  void resolve_success_without_thumbnail_url() {
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "테스트 콘텐츠",
        "설명"
    );

    String result = ContentImageUrlResolver.resolve(content);

    assertThat(result).isEqualTo("/placeholder-movie.png");
  }
}
