package com.team6.moduply.binarycontent.storage.local;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "moduply.storage.local")
public class LocalStorageProperties {

  @NotBlank(message = "로컬 저장소 루트 경로는 필수입니다.")
  private String rootPath;

  @NotBlank(message = "로컬 저장소 URL prefix는 필수입니다.")
  @Pattern(regexp = "^/(?!$).*$", message = "로컬 저장소 URL prefix는 '/'로 시작해야 합니다.")
  private String urlPrefix;
}
