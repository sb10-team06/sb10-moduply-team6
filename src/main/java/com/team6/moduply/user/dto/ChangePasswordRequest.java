package com.team6.moduply.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ChangePasswordRequest {
  @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
  @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\[\\]{}|;:'\",.<>?/~-])[A-Za-z\\d!@#$%^&*()_+=\\[\\]{}|;:'\",.<>?/~-]{8,20}$",
      message = "비밀번호는 8~20자에 영문, 숫자, 특수문자를 포함해야 합니다."
  )
  @Schema(description = "새 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
  private String newPassword;
}
