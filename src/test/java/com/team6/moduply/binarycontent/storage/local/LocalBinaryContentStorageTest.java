package com.team6.moduply.binarycontent.storage.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalBinaryContentStorageTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("로컬 저장소 업로드 시 storageKey 경로에 파일을 저장한다.")
  void upload_success_with_storage_key() throws Exception {
    // given
    LocalBinaryContentStorage storage = createStorage();
    String key = "contents/content-id/thumbnail/image.png";
    byte[] bytes = "image-bytes".getBytes();

    // when
    String result = storage.upload(key, bytes, "image/png");

    // then
    assertThat(result).isEqualTo(key);
    assertThat(Files.readAllBytes(tempDir.resolve(key))).isEqualTo(bytes);
  }

  @Test
  @DisplayName("로컬 저장소 URL 생성 시 urlPrefix와 storageKey를 조합한다.")
  void generateUrl_success_with_storage_key() {
    // given
    LocalBinaryContentStorage storage = createStorage();

    // when
    String result = storage.generateUrl("contents/content-id/thumbnail/image.png", "image/png");

    // then
    assertThat(result).isEqualTo("/uploads/contents/content-id/thumbnail/image.png");
  }

  @Test
  @DisplayName("로컬 저장소 URL 생성 시 urlPrefix 끝 슬래시를 정규화한다.")
  void generateUrl_success_when_url_prefix_has_trailing_slash() {
    // given
    LocalBinaryContentStorage storage = createStorage("/uploads/");

    // when
    String result = storage.generateUrl("contents/content-id/thumbnail/image.png", "image/png");

    // then
    assertThat(result).isEqualTo("/uploads/contents/content-id/thumbnail/image.png");
  }

  @Test
  @DisplayName("로컬 저장소 삭제 시 storageKey 경로의 파일을 삭제한다.")
  void delete_success_with_storage_key() throws Exception {
    // given
    LocalBinaryContentStorage storage = createStorage();
    String key = "contents/content-id/thumbnail/image.png";
    Path filePath = tempDir.resolve(key);
    Files.createDirectories(filePath.getParent());
    Files.write(filePath, "image-bytes".getBytes());

    // when
    String result = storage.delete(key);

    // then
    assertThat(result).isEqualTo(key);
    assertThat(filePath).doesNotExist();
  }

  @Test
  @DisplayName("로컬 저장소 삭제 시 대상 파일이 없어도 storageKey를 반환한다.")
  void delete_success_when_file_does_not_exist() {
    // given
    LocalBinaryContentStorage storage = createStorage();
    String key = "contents/content-id/thumbnail/missing.png";

    // when
    String result = storage.delete(key);

    // then
    assertThat(result).isEqualTo(key);
  }

  @Test
  @DisplayName("로컬 저장소 업로드 시 rootPath 밖으로 벗어나는 storageKey면 예외가 발생한다.")
  void upload_fail_when_storage_key_escapes_root_path() {
    // given
    LocalBinaryContentStorage storage = createStorage();

    // when & then
    assertThatThrownBy(() -> storage.upload("../escape.png", "image-bytes".getBytes(), "image/png"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("로컬 저장소 경로를 벗어날 수 없습니다.");
  }

  private LocalBinaryContentStorage createStorage() {
    return createStorage("/uploads");
  }

  private LocalBinaryContentStorage createStorage(String urlPrefix) {
    LocalStorageProperties properties = new LocalStorageProperties();
    properties.setRootPath(tempDir.toString());
    properties.setUrlPrefix(urlPrefix);

    return new LocalBinaryContentStorage(properties);
  }
}
