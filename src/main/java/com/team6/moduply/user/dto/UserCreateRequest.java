package com.team6.moduply.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@AllArgsConstructor
public class UserCreateRequest {
  @NotBlank(message = "닉네임은 필수입니다.")
  @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
  private String name;

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  private String email;

  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\[\\]{}|;:'\",.<>?/~-])[A-Za-z\\d!@#$%^&*()_+=\\[\\]{}|;:'\",.<>?/~-]{8,20}$",
      message = "비밀번호는 8~20자입니다"
    )
  @NotBlank(message = "비밀번호는 필수입니다.")
  private String password;
}
