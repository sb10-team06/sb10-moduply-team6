package com.team6.moduply.binarycontent.storage.local;

import static org.assertj.core.api.Assertions.assertThat;

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

  private LocalBinaryContentStorage createStorage() {
    LocalStorageProperties properties = new LocalStorageProperties();
    properties.setRootPath(tempDir.toString());
    properties.setUrlPrefix("/uploads");

    return new LocalBinaryContentStorage(properties);
  }
}
