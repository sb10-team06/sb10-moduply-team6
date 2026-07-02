package com.team6.moduply.content.external;

import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import java.net.URI;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class ExternalImageClient {

  private static final String DEFAULT_FILE_NAME = "external-thumbnail";

  private final RestClient restClient;

  public ExternalImageClient(RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.build();
  }

  public ExternalImageFile download(String imageUrl) {
    if (!StringUtils.hasText(imageUrl)) {
      throw new ContentException(
          ContentErrorCode.EXTERNAL_CONTENT_IMAGE_DOWNLOAD_FAILED,
          Map.of("imageUrl", imageUrl == null ? "null" : imageUrl)
      );
    }

    try {
      ResponseEntity<byte[]> response = restClient.get()
          .uri(imageUrl)
          .retrieve()
          .toEntity(byte[].class);

      byte[] bytes = response.getBody();
      MediaType mediaType = response.getHeaders().getContentType();

      return new ExternalImageFile(
          extractFileName(imageUrl),
          mediaType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : mediaType.toString(),
          bytes == null ? new byte[0] : bytes
      );
    } catch (RestClientException | IllegalArgumentException e) {
      throw new ContentException(
          ContentErrorCode.EXTERNAL_CONTENT_IMAGE_DOWNLOAD_FAILED,
          Map.of("imageUrl", imageUrl),
          e
      );
    }
  }

  private String extractFileName(String imageUrl) {
    String path = URI.create(imageUrl).getPath();

    if (!StringUtils.hasText(path) || path.endsWith("/")) {
      return DEFAULT_FILE_NAME;
    }

    String fileName = path.substring(path.lastIndexOf("/") + 1);
    return StringUtils.hasText(fileName) ? fileName : DEFAULT_FILE_NAME;
  }
}
