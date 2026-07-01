package com.team6.moduply.content.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExternalImageClientTest {

  private MockRestServiceServer server;

  private ExternalImageClient externalImageClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    externalImageClient = new ExternalImageClient(builder);
  }

  @Test
  @DisplayName("외부 이미지 URL 다운로드에 성공하면 파일명과 타입과 bytes를 반환한다.")
  void download_success_with_external_image_url() {
    // Given
    byte[] bytes = "image-bytes".getBytes();
    String imageUrl = "https://image.tmdb.org/t/p/w500/poster.jpg";
    server.expect(once(), requestTo(imageUrl))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(bytes, MediaType.IMAGE_JPEG));

    // When
    ExternalImageFile result = externalImageClient.download(imageUrl);

    // Then
    assertThat(result.fileName()).isEqualTo("poster.jpg");
    assertThat(result.contentType()).isEqualTo("image/jpeg");
    assertThat(result.bytes()).isEqualTo(bytes);
    assertThat(result.size()).isEqualTo(bytes.length);
    server.verify();
  }

  @Test
  @DisplayName("외부 이미지 URL이 비어 있으면 다운로드에 실패한다.")
  void download_fail_when_image_url_is_blank() {
    // When & Then
    assertThatThrownBy(() -> externalImageClient.download(" "))
        .isInstanceOfSatisfying(ContentException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ContentErrorCode.EXTERNAL_CONTENT_IMAGE_DOWNLOAD_FAILED)
        );
  }
}
