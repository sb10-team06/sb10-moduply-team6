package com.team6.moduply.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ResetPasswordRequest {
  @NotBlank(message = "이메일은 필수 입력 값입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  @Schema(description = "임시 비밀번호를 발급받을 이메일", requiredMode = Schema.RequiredMode.REQUIRED)
  private String email;
}
