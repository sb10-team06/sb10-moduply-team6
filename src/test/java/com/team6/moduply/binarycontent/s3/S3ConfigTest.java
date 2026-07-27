package com.team6.moduply.binarycontent.s3;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class S3ConfigTest {

  @Test
  @DisplayName("access key와 secret key가 있으면 S3Client를 생성한다.")
  void createS3Client_success_with_static_credentials() {
    S3Config config = new S3Config(createProperties("access-key", "secret-key"));

    assertThatCode(() -> config.s3Client().close()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("access key와 secret key가 없으면 기본 인증 체인으로 S3Client를 생성한다.")
  void createS3Client_success_with_default_credentials_provider() {
    S3Config config = new S3Config(createProperties(null, null));

    assertThatCode(() -> config.s3Client().close()).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource({
      "access-key,",
      ",secret-key"
  })
  @DisplayName("access key와 secret key 중 하나만 있으면 S3Client 생성에 실패한다.")
  void createS3Client_fail_when_only_one_credential_exists(
      String accessKey,
      String secretKey
  ) {
    S3Config config = new S3Config(createProperties(accessKey, secretKey));

    assertThatThrownBy(config::s3Client)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("S3 access key와 secret key는 함께 설정되어야 합니다.");
  }

  @Test
  @DisplayName("region이 없으면 S3Client 생성에 실패한다.")
  void createS3Client_fail_when_region_is_blank() {
    S3Properties properties = createProperties("access-key", "secret-key");
    properties.setRegion(" ");
    S3Config config = new S3Config(properties);

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
    return properties;
  }
}
