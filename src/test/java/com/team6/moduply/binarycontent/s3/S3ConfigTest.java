package com.team6.moduply.binarycontent.s3;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3ConfigTest {

  @Test
  @DisplayName("S3 설정에 access key와 secret key가 있으면 S3Client와 S3Presigner를 생성한다.")
  void createS3Beans_success_with_static_credentials() {
    // given
    S3Config config = new S3Config(createProperties("access-key", "secret-key"));

    // when & then
    assertThatCode(() -> {
      config.s3Client().close();
      config.getS3Presigner().close();
    }).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("S3 설정에 access key와 secret key가 없으면 기본 인증 체인으로 S3Presigner를 생성한다.")
  void createS3Presigner_success_with_default_credentials_provider() {
    // given
    S3Config config = new S3Config(createProperties(null, null));

    // when & then
    assertThatCode(() -> config.getS3Presigner().close()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("S3 설정에 access key와 secret key 중 하나만 있으면 예외가 발생한다.")
  void createS3Beans_fail_when_only_one_credential_exists() {
    // given
    S3Config config = new S3Config(createProperties("access-key", null));

    // when & then
    assertThatThrownBy(config::s3Client)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("S3 access key와 secret key는 함께 설정되어야 합니다.");
  }

  @Test
  @DisplayName("S3 설정에 region이 없으면 예외가 발생한다.")
  void createS3Beans_fail_when_region_is_blank() {
    // given
    S3Properties properties = createProperties("access-key", "secret-key");
    properties.setRegion(" ");
    S3Config config = new S3Config(properties);

    // when & then
    assertThatThrownBy(config::s3Client)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("AWS_S3_REGION 설정이 누락되었습니다.");
  }

  private S3Properties createProperties(String accessKey, String secretKey) {
    S3Properties properties = new S3Properties();
    properties.setAccessKey(accessKey);
    properties.setSecretKey(secretKey);
    properties.setRegion("ap-northeast-2");
    properties.setBucket("test-bucket");
    properties.setPresignedUrlExpiration(600L);

    return properties;
  }
}
