package com.team6.moduply.content.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.content.enums.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentTest {

  @Test
  @DisplayName("콘텐츠 이미지를 변경하면 바이너리 콘텐츠와 썸네일 URL이 함께 갱신된다.")
  void updateContentImage_success_with_url() {
    Content content = new Content(
        null,
        null,
        ContentType.movie,
        "콘텐츠",
        "콘텐츠 설명"
    );
    BinaryContent contentImg = BinaryContent.create(
        "thumbnail.png",
        1024L,
        "image/png",
        "contents/content-id/thumbnail/thumbnail.png"
    );
    String thumbnailUrl =
        "https://bucket.s3.ap-northeast-2.amazonaws.com/contents/content-id/thumbnail/thumbnail.png";

    content.updateContentImage(contentImg, thumbnailUrl);

    assertThat(content.getContentImg()).isSameAs(contentImg);
    assertThat(content.getThumbnailUrl()).isEqualTo(thumbnailUrl);
  }

  @Test
  @DisplayName("콘텐츠 이미지를 다시 변경하면 바이너리 콘텐츠와 썸네일 URL이 새 값으로 함께 갱신된다.")
  void updateContentImage_success_with_replacement() {
    Content content = createContent();
    BinaryContent oldContentImg = BinaryContent.create(
        "old-thumbnail.png",
        1024L,
        "image/png",
        "contents/content-id/thumbnail/old-thumbnail.png"
    );
    BinaryContent newContentImg = BinaryContent.create(
        "new-thumbnail.png",
        2048L,
        "image/png",
        "contents/content-id/thumbnail/new-thumbnail.png"
    );
    content.updateContentImage(oldContentImg, "https://bucket/old-thumbnail.png");

    content.updateContentImage(newContentImg, "https://bucket/new-thumbnail.png");

    assertThat(content.getContentImg()).isSameAs(newContentImg);
    assertThat(content.getThumbnailUrl()).isEqualTo("https://bucket/new-thumbnail.png");
  }

  @SuppressWarnings("deprecation")
  @Test
  @DisplayName("기존 콘텐츠 이미지 변경 메서드를 사용하면 이전 썸네일 URL이 제거된다.")
  void updateContentImg_success_clears_url() {
    Content content = createContent();
    BinaryContent oldContentImg = BinaryContent.create(
        "old-thumbnail.png",
        1024L,
        "image/png",
        "contents/content-id/thumbnail/old-thumbnail.png"
    );
    BinaryContent newContentImg = BinaryContent.create(
        "new-thumbnail.png",
        2048L,
        "image/png",
        "contents/content-id/thumbnail/new-thumbnail.png"
    );
    content.updateContentImage(oldContentImg, "https://bucket/old-thumbnail.png");

    content.updateContentImg(newContentImg);

    assertThat(content.getContentImg()).isSameAs(newContentImg);
    assertThat(content.getThumbnailUrl()).isNull();
  }

  private Content createContent() {
    return new Content(
        null,
        null,
        ContentType.movie,
        "콘텐츠",
        "콘텐츠 설명"
    );
  }
}
