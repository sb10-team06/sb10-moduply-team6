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
  @DisplayName("로컬 저장소 업로드에 성공하면 저장된 이미지 URL을 반환한다.")
  void upload_success_with_image_url() throws Exception {
    LocalBinaryContentStorage storage = createStorage();
    String key = "contents/content-id/thumbnail/image.png";
    byte[] bytes = "image-bytes".getBytes();

    String result = storage.upload(key, bytes, "image/png");

    assertThat(result).isEqualTo("/uploads/" + key);
    assertThat(Files.readAllBytes(tempDir.resolve(key))).isEqualTo(bytes);
  }

  @Test
  @DisplayName("로컬 저장소 업로드 시 URL 접두사의 마지막 슬래시를 정규화한다.")
  void upload_success_when_url_prefix_has_trailing_slash() throws Exception {
    LocalBinaryContentStorage storage = createStorage("/uploads/");
    String key = "contents/content-id/thumbnail/image.png";

    String result = storage.upload(key, "image-bytes".getBytes(), "image/png");

    assertThat(result).isEqualTo("/uploads/" + key);
  }

  @Test
  @DisplayName("로컬 저장소 삭제 시 storageKey 경로의 파일을 삭제한다.")
  void delete_success_with_storage_key() throws Exception {
    LocalBinaryContentStorage storage = createStorage();
    String key = "contents/content-id/thumbnail/image.png";
    Path filePath = tempDir.resolve(key);
    Files.createDirectories(filePath.getParent());
    Files.write(filePath, "image-bytes".getBytes());

    String result = storage.delete(key);

    assertThat(result).isEqualTo(key);
    assertThat(filePath).doesNotExist();
  }

  @Test
  @DisplayName("로컬 저장소 삭제 시 파일이 없어도 storageKey를 반환한다.")
  void delete_success_when_file_does_not_exist() {
    LocalBinaryContentStorage storage = createStorage();
    String key = "contents/content-id/thumbnail/missing.png";

    String result = storage.delete(key);

    assertThat(result).isEqualTo(key);
  }

  @Test
  @DisplayName("로컬 저장소 루트 경로를 벗어나는 storageKey 업로드는 실패한다.")
  void upload_fail_when_storage_key_escapes_root_path() {
    LocalBinaryContentStorage storage = createStorage();

    assertThatThrownBy(() -> storage.upload("../escape.png", "image-bytes".getBytes(), "image/png"))
        .isInstanceOf(IllegalArgumentException.class);
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
