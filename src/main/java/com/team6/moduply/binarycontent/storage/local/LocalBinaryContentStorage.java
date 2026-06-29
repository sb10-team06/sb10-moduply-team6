package com.team6.moduply.binarycontent.storage.local;

import com.team6.moduply.binarycontent.storage.BinaryContentStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "moduply.storage.type", havingValue = "local")
public class LocalBinaryContentStorage implements BinaryContentStorage {

  private final LocalStorageProperties properties;

  @Override
  public String upload(String key, byte[] bytes, String contentType) {
    Path path = resolvePath(key);

    try {
      Files.createDirectories(path.getParent());
      Files.write(path, bytes);
      return key;
    } catch (IOException e) {
      throw new UncheckedIOException("로컬 파일 저장에 실패했습니다. key=" + key, e);
    }
  }

  @Override
  public String generateUrl(String key, String contentType) {
    return normalizeUrlPrefix() + "/" + key;
  }

  @Override
  public String delete(String key) {
    try {
      Files.deleteIfExists(resolvePath(key));
      return key;
    } catch (IOException e) {
      throw new UncheckedIOException("로컬 파일 삭제에 실패했습니다. key=" + key, e);
    }
  }

  private Path resolvePath(String key) {
    return Path.of(properties.getRootPath()).resolve(key).normalize();
  }

  private String normalizeUrlPrefix() {
    String urlPrefix = properties.getUrlPrefix();
    if (urlPrefix.endsWith("/")) {
      return urlPrefix.substring(0, urlPrefix.length() - 1);
    }
    return urlPrefix;
  }
}
